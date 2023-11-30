package com.zorsecyber.bouncer.core.processors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.nifi.annotation.behavior.ReadsAttribute;
import org.apache.nifi.annotation.behavior.ReadsAttributes;
import org.apache.nifi.annotation.behavior.SideEffectFree;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;

import com.nteligen.hq.dhs.siaft.processors.SIAFTUtils;
import com.zorsecyber.bouncer.core.dal.SIAnalysisDAL;

@SideEffectFree
@Tags({ "SIAFT", "Dynamic Analysis Router2" })
@CapabilityDescription("The Dynamic Analysis Router is used to route files to "
		+ "dynamic analysis. Routing is done by setting an  " + "indicator thresh hold count or by set a count of "
		+ "file to bypass.")
@ReadsAttributes({
		@ReadsAttribute(attribute = "CoreAttributes.FILENAME.key()", description = "The filename associated with this flowfile"),
		@ReadsAttribute(attribute = SIAFTUtils.FILE_ATTR_ID, description = "The file ID associated with this flowfile") })
@WritesAttributes({})

public class DynamicAnalysisRouter extends AbstractProcessor {
	public static final Relationship AnalyzeSanitizedFile = new Relationship.Builder().name("analyzeSanitizedFile")
			.description("This relationship will submit the modified file after-Sanitization to" + " Dynamic Analysis.")
			.build();

	public static final Relationship AnalyzeOriginalFile = new Relationship.Builder().name("analyzeOriginalFile")
			.description("This relationship will submit the Orignal file pre-Sanitization to" + " Dynamic Analysis.")
			.build();

	public static final Relationship SkipDynamicAnalysis = new Relationship.Builder().name("skipDynamicAnalysis")
			.description("This relationship should be set to auto terminate.").build();

	public static final Relationship RETRY = new Relationship.Builder().name("Retry").description("Retry").build();

	public static final PropertyDescriptor AnalyzeSuspicious = new PropertyDescriptor.Builder()
			.name("AnalyzeSuspicious")
			.description(
					"Files marked suspicious will be sent to dynamic analysis." + " Default is only malicious files.")
			.required(true).allowableValues("true", "false").defaultValue("false").build();

	public static final PropertyDescriptor AnalyzeEverything = new PropertyDescriptor.Builder()
			.name("AnalyzeEverything").required(true).allowableValues("true", "false").defaultValue("false").build();

	public static final PropertyDescriptor NonConformantIndicator = new PropertyDescriptor.Builder()
			.name("nonConformantIndicator")
			.description("Indicator to send nonconformant files to dynamic analysis."
					+ " Value of false do not send any files. Value of true send all files with "
					+ "nonconformant indicator.")
			.required(false).allowableValues("true", "false").addValidator(StandardValidators.BOOLEAN_VALIDATOR)
			.defaultValue("false").build();

	public static final PropertyDescriptor FilesToBypassCount = new PropertyDescriptor.Builder()
			.name("filesToBypassCount")
			.description("The number of files to skip before sending a file to dynamic anlaysis."
					+ " Value of -1 do not send any files. Value of 0 send all files.")
			.required(false).addValidator(StandardValidators.INTEGER_VALIDATOR).defaultValue("-1").build();

	public static final PropertyDescriptor UseAnalysisOnlyFlow = new PropertyDescriptor.Builder()
			.name("useAnalysisOnlyFlow")
			.description("Indicator used to mark a flowfile as not sanitized in an analysis only flow."
					+ " Value of false mark file sanitzed. Value of true mark file not sanitized "
					+ "nonconformant indicator.")
			.required(false).allowableValues("true", "false").addValidator(StandardValidators.BOOLEAN_VALIDATOR)
			.defaultValue("false").build();

	public static final PropertyDescriptor SendOriginalFile = new PropertyDescriptor.Builder().name("sendOriginalFile")
			.description("Indicates whether or not to send the original file to dynamic analysis.").required(false)
			.allowableValues("true", "false").addValidator(StandardValidators.BOOLEAN_VALIDATOR).defaultValue("true")
			.build();

	public static final PropertyDescriptor PathToOriginalFile = new PropertyDescriptor.Builder()
			.name("pathToOriginalFile").description("The path where the orignal pre-sanitize file is saved.")
			.required(true).addValidator(StandardValidators.FILE_EXISTS_VALIDATOR)
			.defaultValue("/home/nifi/data/original").build();

	@Override
	public List<PropertyDescriptor> getSupportedPropertyDescriptors() {
		List<PropertyDescriptor> properties = new ArrayList<>();
		properties.add(FilesToBypassCount);
		properties.add(NonConformantIndicator);
		properties.add(UseAnalysisOnlyFlow);
		properties.add(SendOriginalFile);
		properties.add(PathToOriginalFile);
		properties.add(AnalyzeSuspicious);
		properties.add(AnalyzeEverything);
		return properties;
	}

	@Override
	public Set<Relationship> getRelationships() {
		return new HashSet<>(Arrays.asList(AnalyzeSanitizedFile, AnalyzeOriginalFile, SkipDynamicAnalysis, RETRY));
	}

	@Override
	protected void init(ProcessorInitializationContext context) {
		super.init(context);
	}

	@Override
	public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {
		FlowFile flowFile = session.get();
		if (flowFile == null) {
			// tag with failure
			session.putAttribute(flowFile, SIAFTUtils.ANALYSIS_SUCCESS, Boolean.FALSE.toString());
			session.transfer(flowFile, SkipDynamicAnalysis);
			return;
		}
		Boolean analyzeSuspicious = context.getProperty("AnalyzeSuspicious").asBoolean();
		Boolean analyzeEverything = context.getProperty("AnalyzeEverything").asBoolean();
		Boolean sendFileToAnalysis = false;
		Long fileAttributeId = Long.parseLong(flowFile.getAttribute(SIAFTUtils.FILE_ATTR_ID));
		Boolean sanitized = false;
		String verdict;
		if (flowFile.getAttribute(SIAFTUtils.SANITIZED) != null) {
			sanitized = true;
		}
		try {
			verdict = SIAnalysisDAL.getStaticAnalysisVerdict(fileAttributeId, sanitized);
		} catch (Exception e) {
//			e.printStackTrace();
			getLogger().warn("No static analysis results for fileAttributeId " + fileAttributeId);
			session.transfer(flowFile, RETRY);
			return;
		}
		getLogger().debug(
				"AttributeID " + Long.toString(fileAttributeId) + " verdict " + verdict + ". Sanitized? " + sanitized);
		if (analyzeEverything || verdict.equals("malicious") || (analyzeSuspicious && verdict.equals("suspicious"))) {
			sendFileToAnalysis = true;
		}
		if (sendFileToAnalysis) {
			if (sanitized) {
				session.transfer(flowFile, AnalyzeSanitizedFile);
				return;
			} else {
				session.transfer(flowFile, AnalyzeOriginalFile);
				return;
			}
		} else {
			session.transfer(flowFile, SkipDynamicAnalysis);
			return;
		}
	}
}
