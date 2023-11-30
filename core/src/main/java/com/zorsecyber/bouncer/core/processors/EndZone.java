package com.zorsecyber.bouncer.core.processors;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.persistence.EntityManager;

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
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;
import org.json.JSONObject;

import com.controlcyberrisk.siaft.dependencies.HttpUtils;
import com.nteligen.hq.dhs.siaft.dal.EntityManagerFactorySingleton;
import com.nteligen.hq.dhs.siaft.dal.FileAttributeDAL;
import com.nteligen.hq.dhs.siaft.dao.FileAttribute;
import com.nteligen.hq.dhs.siaft.processors.SIAFTUtils;
import com.zorsecyber.bouncer.core.dal.FileSubmissionDAL;
import com.zorsecyber.bouncer.core.dal.ReportRepoDAL;
import com.zorsecyber.bouncer.core.dal.TaskDAL;
import com.zorsecyber.bouncer.core.dao.FileSubmission;
import com.zorsecyber.bouncer.core.dao.SIAnalysis;
import com.zorsecyber.bouncer.core.dao.Task;
import com.zorsecyber.bouncer.core.dependencies.SophosIntelixAPI;
import com.zorsecyber.bouncer.core.dependencies.SophosIntelixReport;

/**
 * The EndZone processor is placed at the end of the analysis pipeline. This
 * processor sets the file's fileSubmission status and deletes the original file
 * from the disk.
 *
 * @param context  Provides a bridge between a Processor and the framework.
 *                 Includes information about how the Processor is currently
 *                 configured and allows the Processor to perform
 *                 Framework-specific tasks.
 * @param session  Provides a mechanism by which FlowFiles can be created,
 *                 destroyed, examined, cloned, and transferred to other
 *                 Processors.
 * @param flowFile The FlowFile to process. OUTPUT: The processor outputs the
 *                 input flowfile.
 */
@Tags({ "geruoff", "siaft" })
@CapabilityDescription("End Processor")
@SeeAlso({})
@ReadsAttributes({ @ReadsAttribute(attribute = "", description = "") })
@WritesAttributes({ @WritesAttribute(attribute = "", description = "") })
public class EndZone extends AbstractProcessor {

	public static final PropertyDescriptor SUCCESS_TYPE = new PropertyDescriptor.Builder().name("SUCCESS_TYPE")
			.displayName("Success Type").required(true).allowableValues("True", "False")
			.addValidator(StandardValidators.NON_EMPTY_VALIDATOR).build();

	public static final Relationship SUCCESS = new Relationship.Builder().name("SUCCESS")
			.description("Successfully Processed").build();
	public static final Relationship FAILURE = new Relationship.Builder().name("FAILURE")
			.description("Processing Failed").build();
	public static final Relationship RETRY = new Relationship.Builder().name("RETRY").description("Retry").build();

	private List<PropertyDescriptor> descriptors;

	private Set<Relationship> relationships;

	@Override
	protected void init(final ProcessorInitializationContext context) {
		final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
		descriptors.add(SUCCESS_TYPE);
		this.descriptors = Collections.unmodifiableList(descriptors);

		final Set<Relationship> relationships = new HashSet<Relationship>();
		relationships.add(SUCCESS);
		relationships.add(FAILURE);
		relationships.add(RETRY);
		this.relationships = Collections.unmodifiableSet(relationships);

	}

	@Override
	public Set<Relationship> getRelationships() {
		return this.relationships;
	}

	@Override
	public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
		return descriptors;
	}

	@OnScheduled
	public void onScheduled(final ProcessContext context) {

	}

	/**
	 * Update the fileSubmission status corresponding to the provided flowfile, then
	 * delete the original file corresponding to the provided flowfile from the disk
	 *
	 * @param session  The active NiFi ProcessSession.
	 * @param context  The NiFi ProcessContext.
	 * @param flowFile The FlowFile containing the file to process.
	 * @throws ProcessException There was a problem performing file analysis.
	 */
	@Override
	public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
		FlowFile flowFile = session.get();
		if (flowFile == null) {
			getLogger().error("EndZone detected a null FlowFile");
			context.yield(); // This ensures that the yield duration is honored
			return;
		}
		/** set variables **/
		String filePath = HttpUtils.sanitize_path(flowFile.getAttribute(SIAFTUtils.PATH).toString())
				+ flowFile.getAttribute(SIAFTUtils.FILENAME);
		Boolean successType = Boolean.parseBoolean(context.getProperty(SUCCESS_TYPE).toString());
		Boolean sanitized = false;
//		String fileName = flowFile.getAttribute("filename");

		long submissionId = Long.parseLong(flowFile.getAttribute(SIAFTUtils.SUBMISSION_ID));

		/** set fileSubmission success **/
		try {
			FileSubmissionDAL.SetFileSubmissionSuccess(submissionId, successType);
		} catch (Exception ex) {
			getLogger().error("EndZone failed to modify submissionId " + submissionId);
			ex.printStackTrace();
			session.transfer(flowFile, RETRY);
			session.commit();
			return;
		}

		FileSubmission fs;
		Task task;
		EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
		try {
			fs = FileSubmissionDAL.getFileSubmission(submissionId);
			task = fs.getTask();
		} catch (Exception ex) {
			getLogger().error("EndZone failed to get submission for submissionID " + submissionId);
			/** delete the original file **/
			// delete the file
			File file = new File(filePath);
			if (file.exists()) {
				file.delete();
			}
			ex.printStackTrace();
			session.transfer(flowFile, FAILURE);
			session.commit();
			return;
		}

		/** set verdict **/
		// if successful, write verdict to db
		if (successType) {
			try {
				Boolean fileIsModified = false;
				if (flowFile.getAttribute(SIAFTUtils.SANITIZED) != null) {
					sanitized = true;
					fileIsModified = fs.getSanitizeStatus().equals("sanitized");
				}
				String verdict = "clean";
				String sanitizedVerdict = null;
				FileAttribute fa = FileSubmissionDAL.getFileAttribute(entityManager, fs);
				Set<SIAnalysis> siAnalyses = FileAttributeDAL.getSiAnalyses(entityManager, fa);
				getLogger().info("Found " + siAnalyses.size() + " siAnalyses");
				SIAnalysis s = null;
				SIAnalysis d = null;
				SIAnalysis sanS = null;
				SIAnalysis sanD = null;
				for (SIAnalysis siA : siAnalyses) {
					if (siA.getAnalysisType().equals("static")) {
						if (siA.getSanitized()) {
							if (fileIsModified) {
								sanS = siA;
							}
						} else {
							s = siA;
						}
					}
					if (siA.getAnalysisType().equals("dynamic"))
						if (siA.getSanitized()) {
							sanD = siA;
						} else {
							d = siA;
						}
				}
				if (d != null) {
					if (d.getVerdict().equals("clean")) {
						verdict = "suspicious";
					} else {
						verdict = "malicious";
					}
				}
				if (sanitized) {
					if (!fileIsModified) {
						sanitizedVerdict = "unchanged";
					} else {
						sanitizedVerdict = "clean";
						if (sanD != null) {
							if (sanD.getVerdict().equals("clean")) {
								sanitizedVerdict = "suspicious";
							} else {
								sanitizedVerdict = "malicious";
							}
						}
					}
				}

				// check to see if analysis results are not yet available. If SA verdict !=
				// clean,
				// but no DA results exist, retry until they're available
				String retryMsg = null;
				Boolean retry = false;
				if (!verdictIsCleanOrFailed(s) && d == null) {
					retryMsg = "EndZone: Analysis 1 results unavailable for fileSubmission " + submissionId
							+ ". Retrying...";
					retry = true;
				} else if (sanS != null && !verdictIsCleanOrFailed(sanS) && sanD == null && fileIsModified) {
					retryMsg = "EndZone: Analysis 2 results unavailable for fileSubmission " + submissionId
							+ ". Retrying...";
					retry = true;
				}
				if (retry) {
					getLogger().info(retryMsg);
					session.transfer(flowFile, RETRY);
					session.commit();
					return;
				}
				getLogger().info("Verdict: " + verdict + " Sanitized Verdict: " + sanitizedVerdict
						+ " for submissionId " + submissionId);
				FileSubmissionDAL.setVerdict(submissionId, verdict);
				FileSubmissionDAL.setSanitizedVerdict(submissionId, sanitizedVerdict);
				TaskDAL.incrementCSM(task.getTaskId(), verdict);
			} catch (Exception ex) {
				getLogger().error("EndZone failed to set verdict(s) for submissionId " + submissionId);
				/** delete the original file **/
				// delete the file
				File file = new File(filePath);
				if(!sanitized) {
					deleteFile(file);
				}
				ex.printStackTrace();
				session.transfer(flowFile, FAILURE);
				session.commit();
				return;
			} finally {
				if(entityManager != null) {
					entityManager.close();
				}
			}
		}

		File file;
		try {
			/** increment task CompletedFiles **/
			TaskDAL.incrementCompletedFiles(task.getTaskId());
		} catch (Exception ex) {
			getLogger().error("EndZone failed to increment completedFiles for submissionId " + submissionId);
			ex.printStackTrace();
			session.transfer(flowFile, FAILURE);
			session.commit();
			return;
		} finally {
			/** delete the original file **/
			// delete the file
			file = new File(filePath);
			if(!sanitized) {
				deleteFile(file);
			}
		}
		// if delete failed
		if (!sanitized && file.exists()) {
			getLogger().error("EndZone Could not delete file for submissionId " + submissionId);
			session.transfer(flowFile, FAILURE);
			session.commit();
			return;
		}

		getLogger().info("EndZone deleted file for submissionId " + submissionId);
		// transfer to success
		session.transfer(flowFile, SUCCESS);
		session.commit();
		return;
	}

	private void deleteFile(File file) {
		if (file.exists()) {
			file.delete();
		}
	}
	
	private boolean verdictIsCleanOrFailed(SIAnalysis siA) {
		String verdict = siA.getVerdict();
		return verdict.equals("clean") || verdict.equals("failed");
	}

}
