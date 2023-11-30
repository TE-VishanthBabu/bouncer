package com.zorsecyber.bouncer.core.processors;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
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
import org.json.JSONObject;

import com.microsoft.azure.toolkit.lib.common.logging.Log;
import com.nteligen.hq.dhs.siaft.processors.SIAFTUtils;
import com.zorsecyber.bouncer.core.dal.SIAnalysisDAL;
import com.zorsecyber.bouncer.core.dal.SIReputationDAL;
import com.zorsecyber.bouncer.core.dao.SIAnalysis;
import com.zorsecyber.bouncer.core.dao.SIReputation;
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
@Tags({"geruoff", "Sophos", "siaft"})
@CapabilityDescription("Sophos Intelix DB Writer")
@SeeAlso({})
@ReadsAttributes({@ReadsAttribute(attribute="", description="")})
@WritesAttributes({@WritesAttribute(attribute="", description="")})

public class SophosIntelixDBWriter extends AbstractProcessor {
	
	public static final String INTELLIX_STATIC_ANALYSIS_ENGINE_ID = "1005";
	public static final String JOB_UUID = "siaft.Intellix.Static.job_uuid";
	public static final String  INTELLIX_DATADIR = "/home/nifi/data/intellix/static/reports/";
    
    public static final PropertyDescriptor DATADIR = new PropertyDescriptor
            .Builder().name("DATADIR")
            .displayName("Data Dir")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();
    
	public static final PropertyDescriptor ANALYSIS_TYPE = new PropertyDescriptor.Builder().name("ANALYSIS_TYPE")
			.displayName("Analysis Type").required(true).addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
			.allowableValues("static", "dynamic").build();

    public static final Relationship SUCCESS = new Relationship.Builder()
            .name("SUCCESS")
            .description("Successfully Processed")
            .build();
    public static final Relationship FAILURE = new Relationship.Builder()
            .name("FAILURE")
            .description("Processing Failed")
            .build();

    private List<PropertyDescriptor> descriptors;

    private Set<Relationship> relationships;

    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
        descriptors.add(DATADIR);
		descriptors.add(ANALYSIS_TYPE);
        this.descriptors = Collections.unmodifiableList(descriptors);

        final Set<Relationship> relationships = new HashSet<Relationship>();
        relationships.add(SUCCESS);
        relationships.add(FAILURE);
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
	   * Creates and persists a new SIAnalysis from the analysis report. The analysis
	   * report is the flowfile contents
	   * @param session        The active NiFi ProcessSession.
	   * @param context        The NiFi ProcessContext.
	   * @param flowFile       The FlowFile containing the report data.
	   * @throws ProcessException There was a problem performing file analysis.
	   */
    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
    	FlowFile flowFile = session.get();
		if (flowFile == null) {
			getLogger().error("Intelix detected a null FlowFile");
			context.yield(); // This ensures that the yield duration is honored
			throw new ProcessException("Intelix detected a null FlowFile");
		}
		
		

        /**  set variables **/
      	SIAnalysisDAL intellixDAL = new SIAnalysisDAL();
      	String reportSource;
      	Boolean sanitized = false;
      	Long submissionId = (long) 0;
      	Long fileAttributeId = (long) 0;
      	Long analysisId = (long) 0;

      	
      	/** get the file's fileAttribute and analysis IDs **/
      	fileAttributeId = Long.parseLong(flowFile.getAttribute(SIAFTUtils.FILE_ATTR_ID));
		analysisId = Long.parseLong(flowFile.getAttribute(SIAFTUtils.ANALYSIS_ID));
		submissionId = Long.parseLong(flowFile.getAttribute(SIAFTUtils.SUBMISSION_ID));
		if(flowFile.getAttribute(SIAFTUtils.SANITIZED) != null) {
      		sanitized = true;
      	}
		
		/** create a report object from flow file **/
		JSONObject reportJson;
		try {
			InputStream flowFileContents = session.read(flowFile);
			reportJson = new JSONObject(IOUtils.toString(flowFileContents));
			flowFileContents.close();
		} catch (IOException e) {
			Log.info("Could not read flowFile contents");
			e.printStackTrace();
			session.transfer(flowFile, FAILURE);
				return;
		}
		
		/** generate an SIreport object from JSON **/
		SophosIntelixReport report = new SophosIntelixReport(reportJson);
		// sum SIindicators if static or malicious activity if dynamic
		if (report.analysisType.equals("static")) { 
			report.sumStaticIndicators();
		} else {
			report.sumMaliciousActivity();
		}
		
		/** get the report source **/
		reportSource = flowFile.getAttribute(SIAFTUtils.REPORT_SOURCE).toString();
		
		// compute the verdict
		String verdict;
		if(flowFile.getAttribute(SIAFTUtils.ANALYSIS_SUCCESS).equals("true"))
		{
			verdict = SIAnalysisDAL.determineAnalysisVerdict(report.score, report.analysisType);
		} else {
			verdict = "failed";
			getLogger().info("verdict failed. analysis type "+report.analysisType);
		}
		
		/** create an SIAnalysis from the report file data **/
	     try {
	    	SIAnalysis siAnalysis = intellixDAL.createSIAnalysis(fileAttributeId,
	    			analysisId,
	    			report.jobUuid,
	    			reportSource,
	    			report.status,
	    			report.analysisType,
	    			report.score,
	    			verdict,
	    			report.sha256,
	    			sanitized,
	    			report.staticIndicatorCount,
	    			report.maliciousActivityCount,
	    			report.mimeType,
	    			report.reportLevel,
	    			report.timestamp);
//	    	getLogger().debug("Created SIAnalysis " + siAnalysis);
	    	session.putAttribute(flowFile, SIAFTUtils.INTELIX_ID, Long.toString(siAnalysis.getIntellixId()));
	    	
	    	/** write reputation info to database **/
	    	try {
	    		Date  firstSeen = report.dateFromString(report.reputation.getString("first_seen"));
	    			Date lastSeen = report.dateFromString(report.reputation.getString("last_seen"));
	    			String prevalence = report.reputation.getString("prevalence");
	    			int score = report.reputation.getInt("score");
	    			String scoreString = report.reputation.getString("score_string");
	    			SIReputation reputation = SIReputationDAL.createSIReputation(submissionId, firstSeen, lastSeen, prevalence, score, scoreString);
	    	} catch (Exception e) {
	    		getLogger().debug("No reputation data for siAnalysis " + siAnalysis.getIntellixId());
	    	}
		} catch (Exception e) {
			e.printStackTrace();
			session.transfer(flowFile, FAILURE);
				return;
		}
	     
  		session.transfer(flowFile, SUCCESS);
      	return;
      	
    }

}
