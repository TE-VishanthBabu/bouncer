package com.nteligen.hq.dhs.siaft.processors;

import com.nteligen.hq.dhs.siaft.dal.SanitizeDAL;
import com.nteligen.hq.dhs.siaft.dao.Sanitize;
import com.nteligen.hq.dhs.siaft.exceptions.DatabaseConnectionException;
import com.nteligen.hq.dhs.siaft.exceptions.SIAFTFatalProcessException;
import org.apache.nifi.annotation.behavior.ReadsAttribute;
import org.apache.nifi.annotation.behavior.ReadsAttributes;
import org.apache.nifi.annotation.behavior.SideEffectFree;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.exception.ProcessException;

import java.util.Set;

@SideEffectFree
@Tags({"SIAFT", "Sanitize DB Writer"})
@CapabilityDescription("The Sanitize DB Writer persists file attributes to"
                       + "the database.")
@ReadsAttributes(
  {
    @ReadsAttribute(attribute = "process_success",
                    description = "A boolean indicating success of the sanitization process."),
    @ReadsAttribute(attribute = "process_mime",
                    description = "The mimetype of the file payload after sanitization"),
    @ReadsAttribute(attribute = "process_md5",
                    description = "The MD5 of the file payload after sanitization"),
  })
@WritesAttributes(
    {
      @WritesAttribute(attribute = SanitizeDbWriter.SANITIZE_PK_ID,
                       description = "The primary key to the record representing "
                                      + "this flowfile in the Sanitize table"),
    })

public class SanitizeDbWriter extends SIAFTBaseRetryProcessor
{
  public static final SuccessBehavior SUCCESS_BEHAVIOR = new SuccessBehavior();
  public static final FailureBehavior FAILURE_BEHAVIOR = new FailureBehavior();
  private SanitizeDAL dataLayer;

  public static final String SANITIZE_PK_ID = "sanitize_pk_id";

  @Override
  protected Set<SIAFTBehaviorRetrievable> getBehaviors()
  {
    Set<SIAFTBehaviorRetrievable> behaviors = super.getBehaviors();
    behaviors.add(SUCCESS_BEHAVIOR);
    behaviors.add(FAILURE_BEHAVIOR);
    return behaviors;
  }

  @Override
  protected void initInternal()
  {
    dataLayer = new SanitizeDAL();
  }

  @Override
  public void onTriggerInternal(final ProcessContext context,
                                final ProcessSession session,
                                FlowFile flowFile)
          throws ProcessException, SIAFTFatalProcessException
  {
    long fileAttributeId = Long.parseLong(
        flowFile.getAttribute(SIAFTUtils.FILE_ATTR_ID));
    long sanitizeEngineId = Long.parseLong(
        flowFile.getAttribute(SIAFTUtils.SANITIZE_ENGINE_ID));
    String processSuccess = flowFile.getAttribute("process_success");
    String processMd5 = flowFile.getAttribute("process_md5");
    String processMime = flowFile.getAttribute("process_mime");
    getLogger().debug("Retrieving attributes process_success (" + processSuccess
      + ") process_md5 (" + processMd5 + ") process_mime (" + processMime + ")");

    try
    {
      Sanitize sanitize = dataLayer.createNewSanitize(
        fileAttributeId, sanitizeEngineId,
        processSuccess, processMd5, processMime);
      String sanitizePkId = Long.toString(sanitize.getSanitizeId());

      flowFile = session.putAttribute(flowFile, SANITIZE_PK_ID,
                                      sanitizePkId);
      getLogger().info("Successfully added attribute '{}' to {} with a value "
                       + "of {}; routing to success",
                       new Object[]{ SANITIZE_PK_ID, flowFile,
                                     sanitizePkId});
      session.getProvenanceReporter().modifyAttributes(flowFile);
      session.transfer(flowFile, SUCCESS_BEHAVIOR.successRelationship);
    }
    catch (DatabaseConnectionException ex)
    {
      getLogger().error("Failed to create new Sanitize.", ex);
      flowFile = session.penalize(flowFile);
      getLogger().info("Penalized flowFile (" + flowFile + ") and routing to "
              + RETRY_BEHAVIOR.retryRelationship);
      session.transfer(flowFile, RETRY_BEHAVIOR.retryRelationship);
      return;
    }
    catch (Exception ex) //fault barrier
    {
      getLogger().error("Failed to process {} due to {}; routing to failure",
                        new Object[]{flowFile, ex});
      session.transfer(flowFile, FAILURE_BEHAVIOR.failureRelationship);
    }

  }
}
