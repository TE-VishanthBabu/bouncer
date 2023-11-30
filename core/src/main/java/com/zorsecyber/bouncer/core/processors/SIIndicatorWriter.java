package com.zorsecyber.bouncer.core.processors;

	import java.io.IOException;
import java.io.InputStream;
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
import org.json.JSONObject;

import com.microsoft.azure.toolkit.lib.common.logging.Log;
import com.nteligen.hq.dhs.siaft.exceptions.PersistenceException;
import com.nteligen.hq.dhs.siaft.processors.SIAFTUtils;
import com.zorsecyber.bouncer.core.dal.SIAnalysisDAL;
import com.zorsecyber.bouncer.core.dal.SIIndicatorDAL;
import com.zorsecyber.bouncer.core.dao.SIAnalysis;
import com.zorsecyber.bouncer.core.dao.SIIndicator;
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
	@CapabilityDescription("Sophos Intelix Indicator Writer")
	@SeeAlso({})
	@ReadsAttributes({@ReadsAttribute(attribute="", description="")})
	@WritesAttributes({@WritesAttribute(attribute="", description="")})

	public class SIIndicatorWriter  extends AbstractProcessor {
		
		public static final String INTELLIX_STATIC_ANALYSIS_ENGINE_ID = "1005";
		public static final String JOB_UUID = "siaft.Intellix.Static.job_uuid";
		public static final String  INTELLIX_DATADIR = "/home/nifi/data/intellix/static/reports/";

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
				session.penalize(flowFile);
				throw new ProcessException("Intelix detected a null FlowFile");
			}

	        /**  set variables **/
	      	Long siAnalysisId = (long) 0;
	      	
	      	/** get the file's siAnalysis ID **/
			siAnalysisId = Long.parseLong(flowFile.getAttribute(SIAFTUtils.INTELIX_ID));
			
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
			
			
			/** get indicators **/
			SophosIntelixReport report = new SophosIntelixReport(reportJson);
			try {
			SIIndicatorDAL.persistSIIndicators(report, siAnalysisId);
			} catch (PersistenceException e) {
				e.printStackTrace();
				session.transfer(flowFile, FAILURE);
		      	return;
			}
			
			session.transfer(flowFile, SUCCESS);
	      	return;
	    }
	}
