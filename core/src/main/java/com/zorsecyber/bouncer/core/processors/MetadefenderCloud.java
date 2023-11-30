package com.zorsecyber.bouncer.core.processors;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.nifi.annotation.behavior.ReadsAttribute;
import org.apache.nifi.annotation.behavior.ReadsAttributes;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;
import org.json.JSONObject;

import com.controlcyberrisk.siaft.dependencies.HttpUtils;
import com.microsoft.azure.toolkit.lib.common.logging.Log;
import com.nteligen.hq.dhs.siaft.processors.SIAFTUtils;
import com.zorsecyber.bouncer.core.dal.FileSubmissionDAL;
import com.zorsecyber.bouncer.core.dal.SanitizedFileRepoDAL;
import com.zorsecyber.bouncer.core.dao.FileSubmission;
import com.zorsecyber.bouncer.core.dao.SanitizedFileRepo;
import com.zorsecyber.bouncer.core.dependencies.MetaDefenderReport;
import com.zorsecyber.bouncer.core.dependencies.MetadefenderCloudAPI;
import com.zorsecyber.bouncer.core.dependencies.MetadefenderCloudSubmitResponse;
import com.zorsecyber.bouncer.core.exceptions.MetadefenderException;
import com.zorsecyber.bouncer.core.exceptions.MetadefenderSanitzeBlockedException;
import com.zorsecyber.bouncer.core.exceptions.MetadefenderThrottlingException;

/**
 * Creates and persists SIAnalysis from the Sophos Intelix report pertaining to
 * the provided flowfile.
 *
 * @param session  The active NiFi ProcessSession.
 * @param context  The NiFi ProcessContext.
 * @param flowFile The FlowFile containing the file to process.
 * @throws ProcessException There was a problem performing file analysis.
 */
@Tags({ "geruoff", "DeepSecure", "siaft" })
@CapabilityDescription("DeepSecure Sanitization Processor")
@SeeAlso({})
@ReadsAttributes({ @ReadsAttribute(attribute = "", description = "") })
@WritesAttributes({ @WritesAttribute(attribute = "", description = "") })

public class MetadefenderCloud extends ApiAnalysisProcessor {

	public static final String SANITIZE_ENGINE_ID = "1003";

	public static final PropertyDescriptor REPORT_REPO = new PropertyDescriptor.Builder().name("REPORT_REPO")
			.displayName("Report Repo").required(true).addValidator(StandardValidators.NON_EMPTY_VALIDATOR).build();

	public static final PropertyDescriptor SANITIZED_FILE_REPO = new PropertyDescriptor.Builder()
			.name("SANITIZED_FILE_REPO").displayName("Sanitized File Repo").required(true)
			.addValidator(StandardValidators.NON_EMPTY_VALIDATOR).build();

	public static final PropertyDescriptor REPO_ENABLED = new PropertyDescriptor.Builder().name("REPO_ENABLED")
			.displayName("Search sanitized file repo").required(true)
			.addValidator(StandardValidators.NON_EMPTY_VALIDATOR).allowableValues("true", "false").defaultValue("true")
			.build();

	public static final PropertyDescriptor APIKEY = new PropertyDescriptor.Builder().name("APIKEY")
			.displayName("ApiKey").required(true).addValidator(StandardValidators.NON_EMPTY_VALIDATOR).build();

	public static final Relationship SUCCESS = new Relationship.Builder().name("SUCCESS")
			.description("Successfully Processed").build();
	public static final Relationship FAILURE = new Relationship.Builder().name("FAILURE")
			.description("Processing Failed").build();

	@Override
	protected void init(final ProcessorInitializationContext context) {
		final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
		descriptors.add(APIKEY);
		descriptors.add(REPORT_REPO);
		descriptors.add(SANITIZED_FILE_REPO);
		descriptors.add(ORIGINALS_DIR);
		descriptors.add(REPO_ENABLED);
		this.descriptors = Collections.unmodifiableList(descriptors);

		buildRelationships();
	}

	@OnScheduled
	public void onScheduled(final ProcessContext context) {

	}

	@Override
	public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
		FlowFile flowFile = session.get();
		if (flowFile == null) {
			getLogger().error("Intelix detected a null FlowFile");
			context.yield(); // This ensures that the yield duration is honored
			throw new ProcessException("Intelix detected a null FlowFile");
		}

		/** set variables **/
		String apiKey = context.getProperty(APIKEY).toString();
		File reportRepo = new File(HttpUtils.sanitize_path(context.getProperty(REPORT_REPO).toString()));
		File sanitizedFileRepo = new File(HttpUtils.sanitize_path(context.getProperty(SANITIZED_FILE_REPO).toString()));
		MetadefenderCloudAPI api = new MetadefenderCloudAPI(apiKey, reportRepo);
		String filePath = HttpUtils.sanitize_path(flowFile.getAttribute(SIAFTUtils.PATH).toString())
				+ flowFile.getAttribute(SIAFTUtils.FILENAME);
		long submissionId = Long.parseLong(flowFile.getAttribute(SIAFTUtils.SUBMISSION_ID));
		File fileToSanitize = new File(filePath);
		JSONObject analysisResults;
		String dataId;
		String filename = null;
		String sha256 = null;
		String md5 = null;
		MessageDigest md5Digest = null;
		MetaDefenderReport report;
		File sanitizedFile = null;
		boolean passOriginalFile = false;
		final boolean repoEnabled = Boolean.parseBoolean(context.getProperty(REPO_ENABLED).toString());

		try {
			// get md5 digest instance and file sha256
			FileSubmission fs = FileSubmissionDAL.getFileSubmission(submissionId);
			sha256 = fs.getSha256();
			filename = fs.getFilename();
			md5Digest = MessageDigest.getInstance("MD5");
			md5 = getFileChecksum(md5Digest, fileToSanitize);
			// create file object for sanitized file
			sanitizedFile = new File(new File(sanitizedFileRepo, SanitizedFileRepo.PROVIDER_METADEFENDER), sha256);
		} catch (Exception e) {
			passOriginalFile = true;
		}

		try {
			if (!passOriginalFile) {
				// check repo for file
				getLogger().trace("looking for " + sha256 + " in repo");
				if (repoEnabled
						&& SanitizedFileRepoDAL.sanitizedFileInRepo(sha256, SanitizedFileRepo.PROVIDER_METADEFENDER)) {
					getLogger().info("found sanitized file in repo for " + sha256);
				} else {
					// if not found in repo, sanitize file
					getLogger().info("Sanitizing " + fileToSanitize.getName());
					analysisResults = new JSONObject();
					analysisResults = api.submitFile(fileToSanitize);

					MetadefenderCloudSubmitResponse response = new MetadefenderCloudSubmitResponse(analysisResults);
					getLogger().info("Submitted file : " + analysisResults.toString());
					if (response.hasError()) {
						if (response.getErrorCode() == MetadefenderCloudAPI.ERROR_THROTTLED) {
							throw new MetadefenderThrottlingException(response.getErrorMessages().toString());
						} else {
							throw new MetadefenderSanitzeBlockedException(response.getErrorMessages().toString());
						}
					} else {
						dataId = response.getDataId();
					}
					getLogger().info("Assigned dataid : " + dataId);

					report = new MetaDefenderReport(api.getReport(dataId));
					while (!report.completed) {
//					getLogger().info("Retrieved report");
						report = new MetaDefenderReport(api.getReport(dataId));
					}
//				getLogger().info("Completed report : " + report.toString());
					// create a file container
					sanitizedFile.createNewFile();
					getLogger().info("Saving sanitized file to : " + sanitizedFile.getAbsolutePath());
					// write sanitized file contents to container
					report.saveSanitizedFile(sanitizedFile);
				}
			}
		} catch (MetadefenderThrottlingException e) {
			getLogger().error(e.getClass().getSimpleName() + ": " + e.getMessage());
			session.transfer(flowFile, RETRY);
			session.commit();
			context.yield();
			return;
		} catch (MetadefenderSanitzeBlockedException | IOException e) {
			passOriginalFile = true;
		}

		getLogger().info("Created file object from sanitized file");
		try {
			if (passOriginalFile) {
				// file encountered an Exception or was blocked
				getLogger().warn("File blocked or exception occured: {}", filename);
				session.putAttribute(flowFile, "process_success", "blocked");
			} else {
				// sanitization process was successful
				// check if file was modified
				String sanitizedFileChecksum = getFileChecksum(md5Digest, sanitizedFile);
				if (md5.equals(sanitizedFileChecksum)) {
					// file was unmodified
					getLogger().info("File unmodified: {}", filename);
					session.putAttribute(flowFile, "process_success", "unmodified");

				} else {
					// file was sanitized
					getLogger().info("File was sanitized: {}", filename);
					session.putAttribute(flowFile, "process_success", "sanitized");
					getLogger().info("Sanitized file checksum " + sanitizedFileChecksum + " vs " + md5);
					session.putAttribute(flowFile, "process_md5", sanitizedFileChecksum);
					// change filePath to sanitized file
					session.putAttribute(flowFile, SIAFTUtils.PATH, sanitizedFile.getParent());
					// delete original file
					session.putAttribute(flowFile, SIAFTUtils.FILENAME, sha256);
					if (fileToSanitize.exists()) {
						fileToSanitize.delete();
					}
				}
			}
		} catch (Exception e) {
			session.putAttribute(flowFile, "process_success", "blocked");
		}

		// put sanitize engine Id
		session.putAttribute(flowFile, SIAFTUtils.SANITIZE_ENGINE_ID, SANITIZE_ENGINE_ID);
		session.putAttribute(flowFile, "process_mime", flowFile.getAttribute(SIAFTUtils.MIME_TYPE));
		session.putAttribute(flowFile, SIAFTUtils.SANITIZED, "true");

		session.transfer(flowFile, SUCCESS);
		session.commit();
		return;

	}

}
