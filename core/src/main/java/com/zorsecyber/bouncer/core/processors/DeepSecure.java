package com.zorsecyber.bouncer.core.processors;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
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
import org.apache.tika.Tika;
import org.json.JSONObject;

import com.controlcyberrisk.siaft.dependencies.HttpUtils;
import com.microsoft.azure.toolkit.lib.common.logging.Log;
import com.nteligen.hq.dhs.siaft.exceptions.MimeTypeDetectionException;
import com.nteligen.hq.dhs.siaft.processors.SIAFTUtils;
import com.zorsecyber.bouncer.core.dal.SIAnalysisDAL;
import com.zorsecyber.bouncer.core.dao.SIAnalysis;
import com.zorsecyber.bouncer.core.dependencies.DeepSecureAPI;
import com.zorsecyber.bouncer.core.dependencies.SophosIntelixAPI;
import com.zorsecyber.bouncer.core.dependencies.SophosIntelixReport;

/**
 * Creates and persists SIAnalysis from the Sophos Intelix report
 * pertaining to the provided flowfile.
 *
 * @param session        The active NiFi ProcessSession.
 * @param context        The NiFi ProcessContext.
 * @param flowFile       The FlowFile containing the file to process.
 * @throws ProcessException There was a problem performing file analysis.
 */
@Tags({"geruoff", "DeepSecure", "siaft"})
@CapabilityDescription("DeepSecure Sanitization Processor")
@SeeAlso({})
@ReadsAttributes({@ReadsAttribute(attribute="", description="")})
@WritesAttributes({@WritesAttribute(attribute="", description="")})

public class DeepSecure extends ApiAnalysisProcessor {
	
	public static final String SANITIZE_ENGINE_ID = "1002";
	
    public static final PropertyDescriptor APIKEY = new PropertyDescriptor
            .Builder().name("APIKEY")
            .displayName("ApiKey")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final Relationship SUCCESS = new Relationship.Builder()
            .name("SUCCESS")
            .description("Successfully Processed")
            .build();
    public static final Relationship FAILURE = new Relationship.Builder()
            .name("FAILURE")
            .description("Processing Failed")
            .build();


    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
        descriptors.add(APIKEY);
        descriptors.add(DATADIR);
        descriptors.add(ORIGINALS_DIR);
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

        /**  set variables **/
		String apiKey = context.getProperty(APIKEY).toString();
		String dataDir = context.getProperty(DATADIR).toString();
		Long fileAttributeId = Long.parseLong(flowFile.getAttribute(SIAFTUtils.FILE_ATTR_ID));
		DeepSecureAPI api = new DeepSecureAPI(apiKey, dataDir);
		String filePath = HttpUtils.sanitize_path(flowFile.getAttribute(SIAFTUtils.PATH).toString())
				+ flowFile.getAttribute(SIAFTUtils.FILENAME);
		File fileToSanitize = new File(filePath);
		String md5 = null;
		
		MessageDigest md5Digest;
		try {
			md5Digest = MessageDigest.getInstance("MD5");
			md5 = getFileChecksum(md5Digest, fileToSanitize);
		} catch (NoSuchAlgorithmException | IOException ex) {
			ex.printStackTrace();
			session.transfer(flowFile, FAILURE);
	      	return;
		}
		
		JSONObject analysisResults = new JSONObject();
		try {
			analysisResults = api.submitFile(fileToSanitize);
		} catch (MimeTypeDetectionException | IOException e) {
			e.printStackTrace();
			session.transfer(flowFile, FAILURE);
	      	return;
		}
		
		// put sanitize engine Id
		session.putAttribute(flowFile, SIAFTUtils.SANITIZE_ENGINE_ID, SANITIZE_ENGINE_ID);
		session.putAttribute(flowFile, "process_mime", flowFile.getAttribute(SIAFTUtils.MIME_TYPE));
		getLogger().debug("success : " + analysisResults.getBoolean("success"));
		if (analysisResults.getBoolean("success") == true)
		{
			File sanitizedFile = new File(analysisResults.getString("sanitizedFilePath"));
			try {
				String sanitizedFileChecksum = getFileChecksum(md5Digest, sanitizedFile);
				if (md5.equals(sanitizedFileChecksum))
				{
					session.putAttribute(flowFile, "process_success", "unmodified");
				}
				else {
					session.putAttribute(flowFile, "process_success", "sanitized");
					// change filePath to sanitzed file
					session.putAttribute(flowFile, SIAFTUtils.PATH, sanitizedFile.getParent());
				}
				session.putAttribute(flowFile, "process_md5", sanitizedFileChecksum);
			} catch (IOException e) {
				e.printStackTrace();
				session.transfer(flowFile, FAILURE);
		      	return;
			}
		}
		else
		{
			session.putAttribute(flowFile, "process_success", "sanitization failed");
		}
		getLogger().info("DeepSecure status for FA "+fileAttributeId+" : "+analysisResults.getBoolean("success"));
	     
  		session.transfer(flowFile, SUCCESS);
      	return;
      	
    }

}
