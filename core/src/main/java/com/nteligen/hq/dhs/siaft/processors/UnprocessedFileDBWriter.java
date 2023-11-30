package com.nteligen.hq.dhs.siaft.processors;

import com.nteligen.hq.dhs.siaft.dal.UnprocessedFileDAL;
import com.nteligen.hq.dhs.siaft.dao.UnprocessedFile;
import com.nteligen.hq.dhs.siaft.exceptions.DatabaseConnectionException;
import org.apache.nifi.annotation.behavior.ReadsAttribute;
import org.apache.nifi.annotation.behavior.ReadsAttributes;
import org.apache.nifi.annotation.behavior.SideEffectFree;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.exception.ProcessException;

import java.util.Set;

/**
 * The purpose of this processor is to write to the UnprocessedFile database table. This processor
 * is expecting the following as it's API
 * INPUT:
 *   FlowFlow Attributes
 *   FileAttributeID : This is the primary key for the FileAttribute the UnprocessedFile is
 *   associated with
 *   unprccessed : This is a boolean value meant to represent whether the file was processed or not
 */
@SideEffectFree
@Tags({"SIAFT", "UnprocessedFile DB Writer"})
@CapabilityDescription("The Unprocessed File Database Writer persists a new UnprocessedFile record"
        + " to the database.")
@ReadsAttributes(
  {
    @ReadsAttribute(attribute = SIAFTUtils.FILE_ATTR_ID,
                    description = "The filename associated with this flowfile"),
    @ReadsAttribute(attribute = SIAFTUtils.UNPROCESSED_FILE,
                    description = "Indicates whether the file was processed or not. Valid value "
                            + "is 'true' not case-sensitive for true. Anything else is interpreted"
                            + " as false. Default is false meaning the file was processed."),
  })
public class UnprocessedFileDBWriter extends SIAFTBaseRetryProcessor
{
  public static final SuccessBehavior SUCCESS_BEHAVIOR = new SuccessBehavior();
  public static final FailureBehavior FAILURE_BEHAVIOR = new FailureBehavior();
  private UnprocessedFileDAL dataLayer;

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
    dataLayer = new UnprocessedFileDAL();
  }

  @Override
  public void onTriggerInternal(final ProcessContext context,
                                final ProcessSession session,
                                FlowFile flowFile)
          throws ProcessException
  {
    long fileAttributeId = Long.parseLong(
      flowFile.getAttribute(SIAFTUtils.FILE_ATTR_ID));
    boolean unprocessed = Boolean.parseBoolean(flowFile.getAttribute(
      SIAFTUtils.UNPROCESSED_FILE));
    String unprocessedReason = flowFile.getAttribute(SIAFTUtils.UNPROCESSED_REASON);
    getLogger().debug("Extracted  Unprocessed_file" + unprocessed
      + "from the flow File Attributes.");
    try
    {
      UnprocessedFile unprocessedFile = dataLayer.createUnprocessedFile(
        fileAttributeId, unprocessed, unprocessedReason);
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
