package com.nteligen.hq.dhs.siaft.processors;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
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
import org.apache.nifi.flowfile.attributes.CoreAttributes;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.stream.io.StreamUtils;

@SideEffectFree
@Tags({"SIAFT", "Dynamic Analysis Router"})
@CapabilityDescription("The Dynamic Analysis Router is used to route files to "
                       + "dynamic analysis. Routing is done by setting an  "
                       + "indicator thresh hold count or by set a count of "
                       + "file to bypass.")
@ReadsAttributes(
    {
      @ReadsAttribute(attribute = "CoreAttributes.FILENAME.key()",
          description = "The filename associated with this flowfile"),
      @ReadsAttribute(attribute = SIAFTUtils.FILE_ATTR_ID,
          description = "The file ID associated with this flowfile")
      })
@WritesAttributes(
    {})

public class DynamicAnalysisRouter extends AbstractProcessor
{

  private List<PropertyDescriptor> properties;
  private Set<Relationship> relationships;

  private int counter = 0;

  public static final Relationship AnalyzeSanitizedFile = new Relationship.Builder()
      .name("analyzeSanitizedFile")
      .description("This relationship will submit the modified file after-Sanitization to"
          + " Dynamic Analysis.")
      .build();

  public static final Relationship AnalyzeOriginalFile = new Relationship.Builder()
      .name("analyzeOriginalFile")
      .description("This relationship will submit the Orignal file pre-Sanitization to"
          + " Dynamic Analysis.")
      .build();

  public static final Relationship SkipDynamicAnalysis = new Relationship.Builder()
      .name("skipDynamicAnalysis")
      .description("This relationship should be set to auto terminate.")
      .build();

  public static final PropertyDescriptor IndicatorThreshHold = new PropertyDescriptor.Builder()
      .name("indicatorThreshHold")
      .description("The thresh hold of indicator count to send to dynamic analysis."
                   + " Value of -1 do not send any files. Value of 0 send all files.")
      .required(false)
      .addValidator(StandardValidators.INTEGER_VALIDATOR)
      .defaultValue("-1")
      .build();

  public static final PropertyDescriptor NonConformantIndicator = new PropertyDescriptor.Builder()
      .name("nonConformantIndicator")
      .description("Indicator to send nonconformant files to dynamic analysis."
                   + " Value of false do not send any files. Value of true send all files with "
                   + "nonconformant indicator.")
      .required(false)
      .allowableValues("true","false")
      .addValidator(StandardValidators.BOOLEAN_VALIDATOR)
      .defaultValue("false")
      .build();

  public static final PropertyDescriptor FilesToBypassCount = new PropertyDescriptor.Builder()
      .name("filesToBypassCount")
      .description("The number of files to skip before sending a file to dynamic anlaysis."
                   + " Value of -1 do not send any files. Value of 0 send all files.")
      .required(false)
      .addValidator(StandardValidators.INTEGER_VALIDATOR)
      .defaultValue("-1")
      .build();

  public static final PropertyDescriptor UseAnalysisOnlyFlow = new PropertyDescriptor.Builder()
      .name("useAnalysisOnlyFlow")
      .description("Indicator used to mark a flowfile as not sanitized in an analysis only flow."
                   + " Value of false mark file sanitzed. Value of true mark file not sanitized "
                   + "nonconformant indicator.")
      .required(false)
      .allowableValues("true","false")
      .addValidator(StandardValidators.BOOLEAN_VALIDATOR)
      .defaultValue("false")
      .build();

  public static final PropertyDescriptor SendOriginalFile = new PropertyDescriptor.Builder()
      .name("sendOriginalFile")
      .description("Indicates whether or not to send the original file to dynamic analysis.")
      .required(false)
      .allowableValues("true","false")
      .addValidator(StandardValidators.BOOLEAN_VALIDATOR)
      .defaultValue("true")
      .build();

  public static final PropertyDescriptor PathToOriginalFile = new PropertyDescriptor.Builder()
      .name("pathToOriginalFile")
      .description("The path where the orignal pre-sanitize file is saved.")
      .required(true)
      .addValidator(StandardValidators.FILE_EXISTS_VALIDATOR)
      .defaultValue("")
      .build();

  @Override
  public List<PropertyDescriptor> getSupportedPropertyDescriptors()
  {
    List<PropertyDescriptor> properties = new ArrayList<>();
    properties.add(IndicatorThreshHold);
    properties.add(FilesToBypassCount);
    properties.add(NonConformantIndicator);
    properties.add(UseAnalysisOnlyFlow);
    properties.add(SendOriginalFile);
    properties.add(PathToOriginalFile);

    return properties;
  }

  @Override
  public Set<Relationship> getRelationships()
  {
    return new HashSet<>(Arrays.asList(AnalyzeSanitizedFile, AnalyzeOriginalFile,
        SkipDynamicAnalysis));
  }

  @Override
  protected void init(ProcessorInitializationContext context)
  {
    super.init(context);
  }

  @Override
  public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException
  {
    String indicatorThreshHold = context.getProperty("indicatorThreshHold").toString();
    String numberOfFilesToSkip = context.getProperty("filesToBypassCount").toString();
    Boolean sendNonConformant = context.getProperty("nonConformantIndicator").asBoolean();
    Boolean sendOriginal = context.getProperty("sendOriginalFile").asBoolean();
    String originalFilePath = context.getProperty("pathToOriginalFile").toString();
    Boolean sendFileToAnalysis = false;

    try
    {
      getLogger().trace("Processing session " + session);
      FlowFile flowFile = session.get();
      getLogger().trace("Processing flowfile " + flowFile);

      String originalFileName = flowFile.getAttribute(CoreAttributes.FILENAME.key());
      String originalFile = Paths.get(originalFilePath, originalFileName).toString();
      String fileAttributeID = flowFile.getAttribute(SIAFTUtils.FILE_ATTR_ID);

      int threshHold = Integer.parseInt(indicatorThreshHold);
      if (threshHold > -1)
      {
        long rlCount = 1;
        long cyCount = 0;

        System.out.println("Looking for AttributeID "+ fileAttributeID);
        System.out.println("Found "+rlCount+" indicators");
        getLogger().trace("RLIndicators: "+rlCount);

        if ((rlCount >= threshHold) || (cyCount >= threshHold))
        {
          sendFileToAnalysis = true;
        }
      }

      if (sendNonConformant)
      {
        {
          sendFileToAnalysis = true;
        }
      }

      int bypassValue = Integer.parseInt(numberOfFilesToSkip);
      if (bypassValue > -1)
      {
        counter = 0;
        if (counter < bypassValue)
        {
          ++counter;
        }
        else
        {
          counter = 0;
          sendFileToAnalysis = true;
        }
      }
      if (sendFileToAnalysis)
      {
        session.transfer(flowFile, AnalyzeSanitizedFile);
        File origFile = new File(originalFile);
        if (sendOriginal && origFile.exists())
        {
          FlowFile origFlowFile = session.clone(flowFile, 0, 0);
          try (OutputStream outStream = session.write(origFlowFile))
          {
            try (InputStream inStream = new FileInputStream(origFile))
            {
              StreamUtils.copy(inStream, outStream);
            }
          }
          catch (IOException ex)
          {
            throw new IOException("Error retreiving original un-sanitized file", ex);
          }
          session.transfer(origFlowFile, AnalyzeOriginalFile);
        }
      }
      else
      {
        session.transfer(flowFile, SkipDynamicAnalysis);
      }
    }
    catch (InvalidPathException | IOException | ProcessException ex)
    {
      getLogger().debug("An error occurred " + ex);
    }
  }
}
