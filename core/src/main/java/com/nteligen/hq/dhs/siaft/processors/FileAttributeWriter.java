package com.nteligen.hq.dhs.siaft.processors;

import com.nteligen.hq.dhs.siaft.dal.FileAttributeDAL;
import com.nteligen.hq.dhs.siaft.dao.FileAttribute;
import com.nteligen.hq.dhs.siaft.exceptions.DatabaseConnectionException;
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

import java.io.InputStream;
import java.util.Set;

@SideEffectFree
@Tags({"SIAFT", "File Attribute Writer"})
@CapabilityDescription("The File Attribute Writer persists file attributes to"
                       + "the database.")
@ReadsAttributes(
  {
    @ReadsAttribute(attribute = "filename",
                    description = "The filename associated with this flowfile"),
    @ReadsAttribute(attribute = "mime.type",
                    description = "The mimetype of the file payload"),
    @ReadsAttribute(attribute = "uuid",
                    description = "The UUID associated with this flowfile"),
    @ReadsAttribute(attribute = SIAFTUtils.MIME_TYPE,
                    description = "The mime type extracted via the siaft utils."),
    @ReadsAttribute(attribute = SIAFTUtils.FILE_TYPE),
  })
@WritesAttributes(
  {
    @WritesAttribute(attribute = SIAFTUtils.FILE_ATTR_ID,
                     description = "The primary key to the record representing "
                                    + "this flowfile in the FileAttributes table"),
  })
public class FileAttributeWriter extends SIAFTBaseRetryProcessor
{
  public static final SuccessBehavior SUCCESS_BEHAVIOR = new SuccessBehavior();
  public static final FailureBehavior FAILURE_BEHAVIOR = new FailureBehavior();

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
  }

  @Override
  public void onTriggerInternal(final ProcessContext context,
                                final ProcessSession session,
                                FlowFile flowFile)
    throws ProcessException
  {
    FileAttributeDAL dataLayer = new FileAttributeDAL();
    try
    {
      String fileName = flowFile.getAttribute(CoreAttributes.FILENAME.key());
      long submissionId = Long.parseLong(flowFile.getAttribute(SIAFTUtils.SUBMISSION_ID));
      String fileType = flowFile.getAttribute(SIAFTUtils.FILE_TYPE);
      String uuid = flowFile.getAttribute(CoreAttributes.UUID.key());
      String[] checksums = new String[2];
      String md5sum;
      String sha256;
      try (InputStream inputStream = session.read(flowFile))
      {
    	checksums = SIAFTUtils.calcChecksums(inputStream);
    	md5sum = checksums[0];
    	sha256 = checksums[1];
      }
      FileAttribute fileAttribute = dataLayer.createNewFileAttribute(
              fileName, submissionId, fileType, md5sum, sha256, uuid);
      String fileAttributeId = Long.toString(fileAttribute.getFileAttributeId());

      flowFile = session.putAttribute(flowFile, SIAFTUtils.FILE_ATTR_ID,
              fileAttributeId);
      getLogger().info("Successfully added attribute '{}' to {} with a value "
                      + "of {}; routing to success",
              new Object[]{ SIAFTUtils.FILE_ATTR_ID, flowFile, fileAttributeId});
      session.getProvenanceReporter().modifyAttributes(flowFile);
      session.transfer(flowFile, SUCCESS_BEHAVIOR.successRelationship);
    }
    catch (DatabaseConnectionException ex)
    {
      getLogger().error("Failed to create new FileAttribte.", ex);
      flowFile = session.penalize(flowFile);
      getLogger().info("Penalized flowFile (" + flowFile + ") and routing to "
              + RETRY_BEHAVIOR.retryRelationship);
      session.transfer(flowFile, RETRY_BEHAVIOR.retryRelationship);
      return;
    }
    catch (Exception ex) // fault barrier
    {
      getLogger().error("Failed to process " + flowFile + " routing to "
                      + FAILURE_BEHAVIOR.failureRelationship, ex);
      session.transfer(flowFile, FAILURE_BEHAVIOR.failureRelationship);
      return;
    }
  }
}
