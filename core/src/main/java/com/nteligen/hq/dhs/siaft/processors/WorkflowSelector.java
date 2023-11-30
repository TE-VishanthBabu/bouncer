package com.nteligen.hq.dhs.siaft.processors;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.nifi.annotation.behavior.ReadsAttribute;
import org.apache.nifi.annotation.behavior.ReadsAttributes;
import org.apache.nifi.annotation.behavior.SideEffectFree;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.flowfile.attributes.CoreAttributes;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.exception.ProcessException;
import org.sonatype.aether.util.StringUtils;

import com.nteligen.hq.dhs.siaft.exceptions.InvalidFlowFileException;
import com.zorsecyber.bouncer.core.dependencies.MimeTypeUtils;

@SideEffectFree
@Tags({"SIAFT", "Workflow Selector"})
@CapabilityDescription("The Workflow Selector is the first stage of the SIAFT "
                       + "process. It performs an initial evaluation of each "
                       + "file in the repository to identify the file type and "
                       + "assigns it to a pipeline.")
@ReadsAttributes(
  {
    @ReadsAttribute(attribute = SIAFTUtils.JOB_ID,
                    description = "A unique ID for the inbound job. This must "
                    + "be in UUID format"),
  })
@WritesAttributes(
  {
    @WritesAttribute(attribute = SIAFTUtils.ENTRY,
                     description = "The entry point for the policy"),
    @WritesAttribute(attribute = SIAFTUtils.MIME_TYPE,
            description = "The mime type extracted via the siaft utils."),
    @WritesAttribute(attribute = SIAFTUtils.FILE_TYPE),
    @WritesAttribute(attribute = SIAFTUtils.UNPROCESSED_REASON,
    description = "The reason a file was not processed.")
  })
public class WorkflowSelector extends SIAFTBaseProcessor
{
  public static final SuccessBehavior SUCCESS_BEHAVIOR = new SuccessBehavior();

  private MimeDetector mimeDetector;
  private Set<String> supportedExtensions;

  @Override
  public void initInternal()
  {
    supportedExtensions = new HashSet<>();
    supportedExtensions.add(DynamicRouter.PIPELINE.getName());
    mimeDetector = new MimeDetector();
  }

  @Override
  protected Set<SIAFTBehaviorRetrievable> getBehaviors()
  {
    Set<SIAFTBehaviorRetrievable> behaviors = new HashSet<>();
    behaviors.add(SUCCESS_BEHAVIOR);
    return behaviors;
  }

  private void validateInputs(FlowFile flowfile) throws InvalidFlowFileException
  {
    String jobUuid = flowfile.getAttribute(SIAFTUtils.JOB_ID);
    validateUuid(jobUuid);
  }

  private void validateUuid(String uuid) throws InvalidFlowFileException
  {
    String pattern = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    if (StringUtils.isEmpty(uuid))
    {
      throw new InvalidFlowFileException("Invalid Job UUID [null]");
    }
    else if (!uuid.matches(pattern))
    {
      throw new InvalidFlowFileException("Invalid Job UUID [" + uuid + "]");
    }
  }

  /**
   * The framework calls this method when there is work for this processor. It
   * identifies which pipeline the file should be sent to based on the file
   * extension and content. If the file content matches the given extension it
   * is directed to the associated pipeline. If there is no extension, or the
   * file appears to be masquerading as another type, the file is directed to
   * the error pipeline.
   *
   * @param context Provides a bridge between a Processor and the framework.
   *                Includes information about how the Processor is currently
   *                configured and allos the Processor to perform
   *                Framework-specific tasks.
   * @param session Provides a mechanism by which FlowFiles can be created,
   *                destroyed, examined, cloned, and transferred to other
   *                Processors. In this case, the flowfile is preserved and
   *                transfered along the "success" relationship.
   * @param flowFile The flowfile from the session.
   */
  @Override
  public void onTriggerInternal(final ProcessContext context,
                                final ProcessSession session,
                                FlowFile flowFile)
    throws ProcessException
  {
    getLogger().trace("Processing flowfile " + flowFile);
    if (flowFile == null)
    {
      getLogger().info("Returning because flowfile is null.");
      return;
    }

    try
    {
      validateInputs(flowFile);
      String filename = flowFile.getAttribute(CoreAttributes.FILENAME.key());
      getLogger().debug("Beginning workflow analysis of file [filename="
                        + filename + "]");

      byte[] bytes = readContent(session, flowFile);
      getLogger().debug("Reading flow file content for file [filename="
                        + filename + "]");

//      String nifiMimeType = flowFile.getAttribute(CoreAttributes.MIME_TYPE.key());
//      String extension = FilenameUtils.getExtension(filename);
      String mimeType = mimeDetector.getMimeType(bytes, filename);
      String mimeTypeExtension = mimeDetector.getExtensionFromMimeType(mimeType);

//      InputStream stream = new ByteArrayInputStream(bytes);
//      if (MsOfficeHelper.isPasswordProtected(stream, byteCount))
//      {
//        getLogger().debug("File [filename="
//                          + filename + "] is password protected");
//        session.putAttribute(flowFile, SIAFTUtils.UNPROCESSED_REASON, "Password Protected");
//        flowFile = session.putAttribute(flowFile, SIAFTUtils.ENTRY,
//                                        DynamicRouter.ERROR_PIPELINE.getName());
//      }
//      else 
      if (MimeTypeUtils.allowedMimeType(mimeType))
      {
    	  if(StringUtils.isEmpty(mimeTypeExtension)) mimeTypeExtension = "unmatched";
        getLogger().debug("Nifi determined mime type /'"+mimeType+"/' is accepted for"
            + " [filename=" + filename + ", extension=" + mimeTypeExtension + "]");
        flowFile = session.putAttribute(flowFile, SIAFTUtils.ENTRY, mimeTypeExtension);

        session.putAttribute(flowFile, SIAFTUtils.MIME_TYPE, mimeType);
        session.putAttribute(flowFile, SIAFTUtils.FILE_TYPE, mimeTypeExtension);
      }
      else
      {
        getLogger().debug("MimeType is not supported [filename="
            + filename + "MimeType=" + mimeType + "]");
        flowFile = session.putAttribute(flowFile, SIAFTUtils.ENTRY,
        DynamicRouter.ERROR_PIPELINE.getName());
        session.putAttribute(flowFile, SIAFTUtils.UNPROCESSED_REASON, "Unsupported File Type");
        session.putAttribute(flowFile, SIAFTUtils.MIME_TYPE, mimeType);
        session.putAttribute(flowFile, SIAFTUtils.FILE_TYPE, mimeTypeExtension);
        flowFile = session.putAttribute(flowFile, SIAFTUtils.ENTRY,
                          DynamicRouter.ERROR_PIPELINE.getName());
      }
    }
    catch (Exception ex)
    {
      getLogger().error("An error occurred while processing file.", ex);
      flowFile = session.putAttribute(flowFile, SIAFTUtils.ENTRY,
              DynamicRouter.ERROR_PIPELINE.getName());
    }

    session.getProvenanceReporter().modifyAttributes(flowFile);
    session.transfer(flowFile, SUCCESS_BEHAVIOR.successRelationship);
  }
}
