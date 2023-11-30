package com.zorsecyber.bouncer.core.processors;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.nifi.annotation.behavior.SideEffectFree;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.flowfile.attributes.CoreAttributes;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.InputStreamCallback;
import org.apache.nifi.stream.io.StreamUtils;

import com.controlcyberrisk.siaft.dependencies.HttpUtils;
import com.nteligen.hq.dhs.siaft.processors.SIAFTUtils;
import com.zorsecyber.bouncer.core.dal.FileSubmissionDAL;
import com.zorsecyber.bouncer.core.dao.FileSubmission;
import com.zorsecyber.bouncer.core.dependencies.MimeTypeUtils;

@SideEffectFree
@Tags({ "SIAFT", "Sanitize Router" })
@CapabilityDescription("The Sanitize Router routes files to CDR or EndZone depending on their sanitization status")
//@ReadsAttributes({
//		@ReadsAttribute(attribute = "CoreAttributes.FILENAME.key()", description = "The filename associated with this flowfile"),
//		@ReadsAttribute(attribute = SIAFTUtils.FILE_ATTR_ID, description = "The file ID associated with this flowfile") })
//@WritesAttributes({
//		@WritesAttribute(attribute = VxStreamResultsDBWriter.SANITIZED, description = "True indicates the file has completed the "
//				+ "sanitization, false indicates it has not"), })

public class SanitizeRouter extends AbstractProcessor {
	public static final Relationship sanitizeFile = new Relationship.Builder().name("sanitizeFile")
			.description("Sanitize").build();

	public static final Relationship skipSanitization = new Relationship.Builder().name("skipSanitization")
			.description("Skip Sanitization").build();

	public static final Relationship analyzeSanitizedFile = new Relationship.Builder().name("analyzeSanitizedFile")
			.description("Analyze the sanitized file").build();

	@Override
	public List<PropertyDescriptor> getSupportedPropertyDescriptors() {
		List<PropertyDescriptor> properties = new ArrayList<>();
		return properties;
	}

	@Override
	public Set<Relationship> getRelationships() {
		return new HashSet<>(Arrays.asList(sanitizeFile, skipSanitization, analyzeSanitizedFile));
	}

	@Override
	protected void init(ProcessorInitializationContext context) {
		super.init(context);
	}

	/**
	 * Helper method to read the FlowFile content stream into a byte array.
	 *
	 * @param session  The current process session.
	 * @param flowFile The FlowFile to read the content from.
	 *
	 * @return byte array representation of the FlowFile content.
	 */
	protected byte[] readContent(final ProcessSession session, final FlowFile flowFile) {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream((int) flowFile.getSize() + 1);
		session.read(flowFile, new InputStreamCallback() {
			@Override
			public void process(final InputStream in) throws IOException {
				StreamUtils.copy(in, baos);
			}
		});

		return baos.toByteArray();
	}

	@Override
	public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {
		FlowFile flowFile = session.get();
		if (flowFile == null) {
			// tag with failure
			session.putAttribute(flowFile, SIAFTUtils.ANALYSIS_SUCCESS, Boolean.FALSE.toString());
			session.transfer(flowFile, skipSanitization);
			return;
		}

		String mimeType = flowFile.getAttribute(SIAFTUtils.MIME_TYPE);
		String filename = flowFile.getAttribute(CoreAttributes.FILENAME.key());
		// file representing flowfile
		File file = new File(HttpUtils.sanitize_path(flowFile.getAttribute(SIAFTUtils.PATH).toString())
				+ filename);
		Long submissionId = Long.parseLong(flowFile.getAttribute(SIAFTUtils.SUBMISSION_ID));
		String processSuccess = null;
		Boolean sanitizeEnabled = false;
		Boolean fileIsSanitized = false;
		Boolean fileIsModified = false;
		Boolean compatibleMimeType = false;
		Boolean passwordProtected = false;

		byte[] bytes = readContent(session, flowFile);
		getLogger().debug("Reading flow file content for file [filename=" + filename + "]");

		try {
			// get sanitize enabled
			FileSubmission fileSubmission = FileSubmissionDAL.getFileSubmission(submissionId);
			sanitizeEnabled = fileSubmission.getTask().isSanitize();
			if (sanitizeEnabled) {
				// determine mime type compatibility for CDR
//				compatibleMimeType = true;
				compatibleMimeType = MimeTypeUtils.metadefenderAllowedMimeTypes.contains(mimeType);
				getLogger().debug("mimetype " + mimeType + " compatible for sanitization? "+compatibleMimeType.toString());
					if (flowFile.getAttribute(SIAFTUtils.SANITIZED) != null) {
						fileIsSanitized = true;
						processSuccess = flowFile.getAttribute(SIAFTUtils.PROCESS_SUCCESS);
						// set process success
						FileSubmissionDAL.setSanitizeStatus(fileSubmission.getSubmissionId(), processSuccess);
						if (processSuccess.equals("sanitized")) {
							fileIsModified = true;
						}
					}
			} else {
				session.transfer(flowFile, skipSanitization);
				return;
			}
			// remove process success
			session.removeAttribute(flowFile, SIAFTUtils.PROCESS_SUCCESS);
			
			getLogger().info("sanitizeEnabled "+sanitizeEnabled+", fileIsSanitized "+fileIsSanitized+", fileIsModified "+fileIsModified
					+", compatibleMimeType "+compatibleMimeType+", passwordProtected "+passwordProtected);

			if (!fileIsSanitized && compatibleMimeType && !passwordProtected) {
				getLogger().info("sanitizefile");
				session.transfer(flowFile, sanitizeFile);
				return;
			} else if ((fileIsSanitized && !fileIsModified)
					|| (!fileIsSanitized && (!compatibleMimeType || passwordProtected))) {
				getLogger().info("skipSanitization");
				session.transfer(flowFile, skipSanitization);
				return;
			} else {
				getLogger().info("analyzeSanitizedFile");
				session.transfer(flowFile, analyzeSanitizedFile);
				return;
			}
		} catch (Exception e) {
			e.printStackTrace();
			session.putAttribute(flowFile, SIAFTUtils.ANALYSIS_SUCCESS, Boolean.FALSE.toString());
			session.transfer(flowFile, skipSanitization);
			return;
		}
	}
}
