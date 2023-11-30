package com.zorsecyber.bouncer.core.processors;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;

import com.nteligen.hq.dhs.siaft.processors.SIAFTFileProcessor;
import com.nteligen.hq.dhs.siaft.processors.SIAFTUtils;
import com.zorsecyber.bouncer.core.dependencies.ApiAnalysisReport;
import com.zorsecyber.bouncer.core.exceptions.ApiAnalysisReportException;

public class ApiAnalysisProcessor extends AbstractProcessor {
	
	public static final PropertyDescriptor DATADIR = new PropertyDescriptor.Builder().name("DATADIR")
			.displayName("Data Dir").required(true).addValidator(StandardValidators.NON_EMPTY_VALIDATOR).build();

	public static final PropertyDescriptor ORIGINALS_DIR = new PropertyDescriptor.Builder().name("ORIGINALS_DIR")
			.displayName("Orignal Files Dir").required(true).addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
			.build();
	
	public static final PropertyDescriptor TIMEOUT = new PropertyDescriptor.Builder().name("TIMEOUT")
			.displayName("Timeout").required(true).addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
			.build();
	
	public static final Relationship SUCCESS = new Relationship.Builder().name("SUCCESS")
			.description("Successfully Processed").build();
	public static final Relationship FAILURE = new Relationship.Builder().name("FAILURE")
			.description("Processing Failed").build();
	public static final Relationship RETRY = new Relationship.Builder().name("RETRY")
			.description("Retry").build();
	public static final Relationship REPORT = new Relationship.Builder().name("REPORT")
			.description("Report").build();
	
	private Set<Relationship> relationships;
	protected List<PropertyDescriptor> descriptors;

	@Override
	public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException, ApiAnalysisReportException {}
	
	@Override
	public Set<Relationship> getRelationships() {
		return this.relationships;
	}

	@Override
	public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
		return descriptors;
	}

	@OnScheduled
	public void onScheduled(final ProcessContext context) {}
	
	protected void buildRelationships()
	{
		final Set<Relationship> relationships = new HashSet<Relationship>();
		relationships.add(SUCCESS);
		relationships.add(FAILURE);
		relationships.add(RETRY);
		relationships.add(REPORT);
		this.relationships = Collections.unmodifiableSet(relationships);
	}
	
	protected FlowFile setFlowFileContents(ProcessSession session, ApiAnalysisReport report, FlowFile flowFile)
	{
		return report.writeContentToFlowFile(session, flowFile);
	}
	
	/**
	 * Computes the checksum of a file. Hash type is set by the digest parameter.
	 *
	 * @param digest The message digest used to generate the checksum.
	 * @param file   The file whose checksum will be computed.
	 * @throws IOException There was a problem reading the file.
	 */
	protected static String getFileChecksum(MessageDigest digest, File file) throws IOException {
		// Get file input stream for reading the file content
		FileInputStream fis = new FileInputStream(file);

		// Create byte array to read data in chunks
		byte[] byteArray = new byte[1024];
		int bytesCount = 0;

		// Read file data and update in message digest
		while ((bytesCount = fis.read(byteArray)) != -1) {
			digest.update(byteArray, 0, bytesCount);
		}
		;

		// close the stream; We don't need it now.
		fis.close();

		// Get the hash's bytes
		byte[] bytes = digest.digest();

		// This bytes[] has bytes in decimal format;
		// Convert it to hexadecimal format
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < bytes.length; i++) {
			sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
		}

		// return complete hash
		return sb.toString();
	}


}
