package com.nteligen.hq.dhs.siaft.processors;

import com.nteligen.hq.dhs.siaft.dal.AnalysisDAL;
import com.nteligen.hq.dhs.siaft.dao.Analysis;
import com.nteligen.hq.dhs.siaft.exceptions.DatabaseConnectionException;
import com.nteligen.hq.dhs.siaft.exceptions.PersistenceException;
import com.nteligen.hq.dhs.siaft.exceptions.SIAFTFatalProcessException;
import org.apache.commons.lang.StringUtils;
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

/**
 * The purpose of this processor is to write to the Analysis database table. This processor
 * is expecting the following as it's API
 * INPUT:
 *   FlowFlow Attributes
 *   FileAttributeID : This is the primary key for the FileAttribute the UnprocessedFile is
 *   associated with.
 *   AnalysisEngineID : This is the primary key for the AnalysisEngine that the file was processed
 *   by.
 *   SanitizeEngineID : This is the primary key for the SanitizeEngine that the file was processed
 *   by.
 *   Success : this is a boolean value indicating whether the analysis was successful.
 *   ErrorInfo : This is a string that can accompany the success flag to give more information about
 *   why the Analysis Engine failed to analyze the file.
 * OUTPUT:
 *   There is no output of this processor.
 */
@SideEffectFree
@Tags({"SIAFT", "Analysis DB Writer"})
@CapabilityDescription("The Analysis Database Writer persists a new Analysis record to the "
        + "database.")
@ReadsAttributes(
  {
    @ReadsAttribute(attribute = SIAFTUtils.FILE_ATTR_ID,
                    description = "The file ID associated with this flowfile"),
    @ReadsAttribute(attribute = SIAFTUtils.ANALYSIS_ENGINE_ID,
                    description = "The primary key of the Analysis Engine that the file was "
                            + "processed by. This value must be populated."),
    @ReadsAttribute(attribute = SIAFTUtils.SANITIZE_ENGINE_ID,
                    description = "The primary key of the Sanitize Engine that the file was "
                          + "processed by. This value can be empty."),
    @ReadsAttribute(attribute = AnalysisDBWriter.ANALYSIS_SUCCESS,
                    description = "Whether the anylsis of the file was successful."),
    @ReadsAttribute(attribute = AnalysisDBWriter.ANALYSIS_RESULTS,
            description = "The results of the analysis including any error messages. "
                    + "This can be empty.")
  })
@WritesAttributes(
        {
          @WritesAttribute(attribute = AnalysisDBWriter.ANALYSIS_ID,
                  description = "This is the primary key of the newly created Analysis row in the"
                          + " database.")
        })
public class AnalysisDBWriter extends SIAFTBaseRetryProcessor
{
  public static final String ANALYSIS_SUCCESS = "siaft.AnalysisDBWriter.analysisSuccess";
  public static final String ANALYSIS_RESULTS = "siaft.AnalysisDBWriter.Results";
  public static final String ANALYSIS_ID = "siaft.AnalysisDBWriter.AnalysisID";
  public static final SuccessBehavior SUCCESS_BEHAVIOR = new SuccessBehavior();
  public static final FailureBehavior FAILURE_BEHAVIOR = new FailureBehavior();
  private AnalysisDAL analysisDAL;

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
    this.analysisDAL = new AnalysisDAL();
  }

  @Override
  public void onTriggerInternal(final ProcessContext context,
                                final ProcessSession session,
                                FlowFile flowFile)
          throws ProcessException, SIAFTFatalProcessException
  {	
    long fileAttributeID;
    long analysisEngineID;
    Long sanitizeEngineID;
    try
    {
      fileAttributeID = Long.parseLong(flowFile.getAttribute(SIAFTUtils.FILE_ATTR_ID));
      getLogger().debug("Extracted fileAttributeID " + fileAttributeID + "from the flow File "
              + "Attributes.");
    }
    catch (NumberFormatException ex)
    {
      getLogger().error("When converting the " + SIAFTUtils.FILE_ATTR_ID
              + " from FlowFile Attributes " + flowFile.getAttribute(SIAFTUtils.FILE_ATTR_ID)
              + " failed to be converted to a valid long value.", ex);
      session.transfer(flowFile, FAILURE_BEHAVIOR.failureRelationship);
      return;
    }

    try
    {
      analysisEngineID = Long.parseLong(flowFile.getAttribute(SIAFTUtils.ANALYSIS_ENGINE_ID));
//      getLogger().debug("Extracted analysisEngineID " + analysisEngineID + "from the flow File "
//              + "Attributes.");
    }
    catch (NumberFormatException ex)
    {
      getLogger().error("When converting the " + SIAFTUtils.ANALYSIS_ENGINE_ID
              + " from FlowFile Attributes " + flowFile.getAttribute(SIAFTUtils.ANALYSIS_ENGINE_ID)
              + " failed to be converted to a valid long value.", ex);
      session.transfer(flowFile, FAILURE_BEHAVIOR.failureRelationship);
      return;
    }

    if (!StringUtils.isEmpty(flowFile.getAttribute(SIAFTUtils.SANITIZE_ENGINE_ID)))
    {
      try
      {
        sanitizeEngineID = Long.parseLong(flowFile.getAttribute(SIAFTUtils.SANITIZE_ENGINE_ID));
//        getLogger().debug("Extracted sanitizeEngineID " + sanitizeEngineID + "from the flow File "
//                + "Attributes.");
      }
      catch (NumberFormatException ex)
      {
        getLogger().error("When converting the " + SIAFTUtils.SANITIZE_ENGINE_ID
                + " from FlowFile Attributes "
                + flowFile.getAttribute(SIAFTUtils.SANITIZE_ENGINE_ID)
                + " failed to be converted to a valid long value.", ex);
        session.transfer(flowFile, FAILURE_BEHAVIOR.failureRelationship);
        return;
      }
    }
    else // Sanitize Engine ID is either null or empty
    {
      sanitizeEngineID = null;
    }

    String results = flowFile.getAttribute(AnalysisDBWriter.ANALYSIS_RESULTS);
//    getLogger().debug("Extracted results " + results + "from the flow File Attributes.");
    boolean success = Boolean.parseBoolean(
            flowFile.getAttribute(AnalysisDBWriter.ANALYSIS_SUCCESS));
//    getLogger().debug("Extracted success " + success + "from the flow File Attributes.");

    try
    {
      Analysis analysis = this.analysisDAL.createAnalysis(fileAttributeID,
                                                          analysisEngineID,
                                                          sanitizeEngineID,
                                                          success,
                                                          results);
//      getLogger().debug("Created Analysis " + analysis);
      session.putAttribute(flowFile,
              AnalysisDBWriter.ANALYSIS_ID,
              Long.toString(analysis.getAnalysisId()));
      session.getProvenanceReporter().modifyAttributes(flowFile);
      getLogger().debug("Transferring " + flowFile + " to " + SUCCESS_BEHAVIOR.successRelationship);
      session.transfer(flowFile, SUCCESS_BEHAVIOR.successRelationship);
    }
    catch (DatabaseConnectionException ex)
    {
      getLogger().error("Failed to create new Analysis.", ex);
      flowFile = session.penalize(flowFile);
      getLogger().info("Penalized flowFile (" + flowFile + ") and routing to "
              + RETRY_BEHAVIOR.retryRelationship);
      session.transfer(flowFile, RETRY_BEHAVIOR.retryRelationship);
      return;
    }
    catch (ProcessException ex)
    {
      getLogger().error("Failed to process {} due to {}; routing to failure",
                        new Object[]{flowFile, ex});
      session.transfer(flowFile, FAILURE_BEHAVIOR.failureRelationship);
    }
    catch (PersistenceException ex)
    {
      getLogger().error("Failed to persist Analysis record to the database with the "
              + "fileAttributeID(" + fileAttributeID + "), analysisEngineID(" + analysisEngineID
              + "), sanitizeEngineID(" + sanitizeEngineID
              + "), success(" + success
              + "), results(" + results
              + ") : ", ex);
      session.transfer(flowFile, FAILURE_BEHAVIOR.failureRelationship);
    }
    catch (Exception ex) // fault barrier
    {
      getLogger().error("Failed to persist Analysis record to the database with the "
              + "fileAttributeID(" + fileAttributeID + "), analysisEngineID(" + analysisEngineID
              + "), sanitizeEngineID(" + sanitizeEngineID
              + "), success(" + success
              + "), results(" + results
              + ") : ", ex);
      session.transfer(flowFile, FAILURE_BEHAVIOR.failureRelationship);
    }
  }
}
