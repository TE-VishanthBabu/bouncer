package com.zorsecyber.bouncer.core.processors;

import static com.zorsecyber.bouncer.core.dependencies.SophosIntelixAPI.STATUS_ERROR;
import static com.zorsecyber.bouncer.core.dependencies.SophosIntelixAPI.STATUS_SUCCESS;

import java.io.File;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.apache.nifi.annotation.behavior.ReadsAttribute;
import org.apache.nifi.annotation.behavior.ReadsAttributes;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;
import org.json.JSONObject;

import com.controlcyberrisk.siaft.dependencies.HttpUtils;
import com.nteligen.hq.dhs.siaft.processors.SIAFTUtils;
import com.zorsecyber.bouncer.core.dal.ReportRepoDAL;
import com.zorsecyber.bouncer.core.dependencies.SophosIntelixAPI;
import com.zorsecyber.bouncer.core.dependencies.SophosIntelixReport;
import com.zorsecyber.bouncer.core.exceptions.ApiAnalysisException;
import com.zorsecyber.bouncer.core.exceptions.ApiAnalysisReportException;

/**
 * The purpose of this processor is to process a file using Sophos Intelix.
 * OUTPUT: The processor outputs the input flowFile.
 */
@Tags({ "geruoff", "Sophos", "siaft" })
@CapabilityDescription("Sophos Intelix Static Analysis")
@SeeAlso({})
@ReadsAttributes({ @ReadsAttribute(attribute = "", description = "") })
@WritesAttributes({ @WritesAttribute(attribute = "", description = "") })
public class SophosIntelixAnalysisProcessor extends ApiAnalysisProcessor {

	public static final String INTELLIX_STATIC_ANALYSIS_ENGINE_ID = "1005";
	public static final String INTELLIX_DYNAMIC_ANALYSIS_ENGINE_ID = "1006";

	public static final String INTELLIX_DATADIR = "/home/nifi/data/intellix/static/reports/";

	public static final PropertyDescriptor ACCESS_KEY = new PropertyDescriptor.Builder().name("ACCESS_KEY")
			.displayName("Access Key").required(true).addValidator(StandardValidators.NON_EMPTY_VALIDATOR).build();

	public static final PropertyDescriptor SECRET_KEY = new PropertyDescriptor.Builder().name("SECRET_KEY")
			.displayName("Secret Key").required(true).addValidator(StandardValidators.NON_EMPTY_VALIDATOR).build();

	public static final PropertyDescriptor ANALYSIS_TYPE = new PropertyDescriptor.Builder().name("ANALYSIS_TYPE")
			.displayName("Analysis Type").required(true).addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
			.allowableValues("static", "dynamic").build();
	
	public static final PropertyDescriptor REPO_ENABLED = new PropertyDescriptor.Builder().name("REPO_ENABLED")
			.displayName("Search report repo").required(true).addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
			.allowableValues("true", "false").defaultValue("true").build();
	
	public static final PropertyDescriptor INTELIX_CACHE_ENABLED = new PropertyDescriptor.Builder().name("INTELIX_CACHE_ENABLED")
			.displayName("Search Intelix report cache").required(true).addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
			.allowableValues("true", "false").defaultValue("true").build();

	@Override
	protected void init(final ProcessorInitializationContext context) {
		descriptors = new ArrayList<PropertyDescriptor>();
		descriptors.add(ACCESS_KEY);
		descriptors.add(SECRET_KEY);
		descriptors.add(DATADIR);
		descriptors.add(ORIGINALS_DIR);
		descriptors.add(ANALYSIS_TYPE);
		descriptors.add(REPO_ENABLED);
		descriptors.add(INTELIX_CACHE_ENABLED);
		descriptors.add(TIMEOUT);
		descriptors = Collections.unmodifiableList(descriptors);
		buildRelationships();
	}

	/**
	 * Process the file contained with the provided FlowFile using Sophos Intelix.
	 * This is accomplished by making calls to the Intelix API. The resulting
	 * analysis report is saved to the reports repo.
	 *
	 * @param session  The active NiFi ProcessSession.
	 * @param context  The NiFi ProcessContext.
	 * @param flowFile The FlowFile containing the file to process.
	 * @throws ProcessException           There was a problem performing file
	 *                                    analysis.
	 * @throws ApiAnalysisReportException
	 */
	@Override
	public void onTrigger(final ProcessContext context, final ProcessSession session)
			throws ProcessException, ApiAnalysisReportException {
		FlowFile flowFile = session.get();
		if (flowFile == null) {
			getLogger().error("Intelix detected a null FlowFile");
			context.yield(); // This ensures that the yield duration is honored
			session.penalize(flowFile);
			throw new ProcessException("Intelix detected a null FlowFile");
		}
		// strip analysisID
//		session.removeAttribute(flowFile, SIAFTUtils.ANALYSIS_ID);
		// set variables
		final String analysisType = context.getProperty(ANALYSIS_TYPE).toString();
		final String access_key = context.getProperty(ACCESS_KEY).toString();
		final String secret_key = context.getProperty(SECRET_KEY).toString();
		// options to toggle searching report repo and intelix cache (for debugging use)
		final boolean repoEnabled = Boolean.parseBoolean(context.getProperty(REPO_ENABLED).toString());
		final boolean intelixCacheEnabled = Boolean.parseBoolean(context.getProperty(INTELIX_CACHE_ENABLED).toString());
		String datadir = HttpUtils.sanitize_path(context.getProperty(DATADIR).toString());
		String persist_dir = datadir + HttpUtils.sanitize_path("reports") + analysisType;
		String job_uuid = new String();
		String engineId = null;
		String reportSource = "";
		SophosIntelixReport report = null;
		String sha256 = null;
		Boolean completed = false;
		Boolean written = false;
		Boolean success = true;
		// timeout 1 or 15 mins
		long jobTimeout = Long.parseLong(context.getProperty(TIMEOUT).toString());
		long seconds_elapsed = 0;

		// tag flowfile with SophosIntellix Analysis Engine ID
		if (analysisType.equals("static")) {
			engineId = INTELLIX_STATIC_ANALYSIS_ENGINE_ID;
		} else {
			engineId = INTELLIX_DYNAMIC_ANALYSIS_ENGINE_ID;
		}
		session.putAttribute(flowFile, SIAFTUtils.ANALYSIS_ENGINE_ID, engineId);
		// create API object
		SophosIntelixAPI intellixAPI = new SophosIntelixAPI(access_key, secret_key);
		// Submit file to API
		String fileToSubmit = HttpUtils.sanitize_path(flowFile.getAttribute(SIAFTUtils.PATH).toString())
				+ flowFile.getAttribute(SIAFTUtils.FILENAME);
		// see if this file has already been analyzed
		try {
			// compute SHA-1 checksum
			MessageDigest shaDigest = MessageDigest.getInstance("SHA-256");
			sha256 = getFileChecksum(shaDigest, new File(fileToSubmit));
			// First check our local report repository for this sha256
			if (repoEnabled && ReportRepoDAL.reportExists(sha256, analysisType)) {
				report = SophosIntelixReport.getReportFromFile(sha256, datadir, analysisType);
				written = true;
				getLogger().debug("Found [" + analysisType + "/" + sha256 + "] in reports repo");
				completed = true;
				reportSource = "repo";
			} else if(intelixCacheEnabled) {
				// if not in report repo, check if file has been analyzed by intelix
				// get report by checksum
				report = new SophosIntelixReport(intellixAPI.getReport("", sha256, analysisType));
				if (report.status.equals(STATUS_SUCCESS)) {
					getLogger().debug(sha256 + " has already been analyzed by intelix");
					completed = true;
					reportSource = "intelix (database)";
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			getLogger().warn("Exception : " + e.getMessage());
			success = false;
			completed = true;
//			session.transfer(flowFile, FAILURE);
//			return;
		}
		// tag flowfile with sha256 checksum
		session.putAttribute(flowFile, SophosIntelixAPI.SHA_256, sha256);

		// if the file has not been analyzed already
		if (!completed) {
			getLogger().debug("submitting " + sha256 + " to IntellixAPI ("+analysisType+")");
			JSONObject intellixJSONResponse;
			try {
				// check if the submission rate limit has been hit
				int submits_left = intellixAPI.getSubmitsLeft(new String[] { "files", analysisType });
				// if not, wait until a submission is available
				while (submits_left < 10) {
					TimeUnit.SECONDS.sleep(100);
					submits_left = intellixAPI.getSubmitsLeft(new String[] { "files", analysisType });
				}
				// submit the file for analysis
				intellixJSONResponse = intellixAPI.submitFile(fileToSubmit, analysisType);
				
				// create report object
				getLogger().debug("Creating report object from jsonresponse");
				report = new SophosIntelixReport(intellixJSONResponse);
				job_uuid = report.jobUuid;
			} catch (ApiAnalysisException | InterruptedException e) {
				getLogger().warn("Exception: " + e.getMessage());
				e.printStackTrace();
//				session.transfer(flowFile, FAILURE);
//				return;
				success = false;
			}

			// wait for the analysis to complete
			long seconds_start = System.currentTimeMillis();
			do {
				try {
					// retrieve the job status from intelix API
					report = new SophosIntelixReport(intellixAPI.getReport(job_uuid, "", analysisType));
				} catch (ApiAnalysisReportException e) {
					getLogger().warn("Exception: " + e.getMessage());
					// if report retrieval fails or status is error
					success = false;
					break;
				}
				// sleep if analysis is not finished
				try {
					if (analysisType.equals("dynamic") && seconds_elapsed > 0.1) {
						TimeUnit.SECONDS.sleep(50);
					}
					TimeUnit.MILLISECONDS.sleep(100);
				} catch (InterruptedException e) {}
				// update elapsed time counter
				seconds_elapsed = (System.currentTimeMillis() - seconds_start) / 1000;
				// loop until the analysis is finished
			} while (!report.status.equals(STATUS_SUCCESS) && seconds_elapsed < jobTimeout);
			getLogger().debug("retrieved report from IntellixAPI");

			// if the analysis timed out, redirect to failure
			if (seconds_elapsed >= jobTimeout) {
				getLogger().debug("reached processing timeout");
//				session.transfer(flowFile, FAILURE);
				success = false;
			}
			reportSource = "intelix (analysis)";
		}

		// save analysis report to file
			if (success && report !=null && !written) {
				report.saveReportToFile(persist_dir);
			}
			if(report == null) {
				report = SophosIntelixReport.createFailedReportTemplate(analysisType);
			} else if (!success) {
				// put analysis type into report if analysis failed
				report = new SophosIntelixReport(report.json.put("analysis_type", analysisType));
			}
			// create a flowfile containing the report JSON data
			FlowFile reportFile = session.clone(flowFile, 0, 0);
			reportFile = setFlowFileContents(session, report, reportFile);
			session.putAttribute(reportFile, SIAFTUtils.ANALYSIS_SUCCESS, success.toString());
			session.putAttribute(reportFile, SIAFTUtils.REPORT_SOURCE, reportSource);
			session.transfer(reportFile, REPORT);

		// transfer flowfile to success
		session.putAttribute(flowFile, SIAFTUtils.ANALYSIS_SUCCESS, success.toString());
		session.transfer(flowFile, SUCCESS);
		session.commit();
		return;
	}

}
