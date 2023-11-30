package com.nteligen.hq.dhs.siaft.processors;

import com.nteligen.hq.dhs.siaft.dal.AnalyzeEngineDAL;
import com.nteligen.hq.dhs.siaft.dao.AnalyzeEngine;
import com.nteligen.hq.dhs.siaft.exceptions.AnalysisException;
import com.nteligen.hq.dhs.siaft.exceptions.FileTimeoutException;
import com.nteligen.hq.dhs.siaft.exceptions.PersistenceException;
import com.nteligen.hq.dhs.siaft.exceptions.SessionException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.flowfile.attributes.CoreAttributes;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.exception.ProcessException;

import java.io.IOException;

public abstract class SIAFTAnalysisProcessor extends SIAFTFileProcessor
{
  private AnalyzeEngine analyzeEngine = null;

  public static final String ANALYSIS_ENGINE_NAME_PROPERTY = "Analysis Engine Name";

  /**
   * Process the file contained with the provided FlowFile. This is
   * accomplished by executing put and get actions to/from the analysis host.
   * The resulting analysis report is placed within the content of the
   * reportFlowFile.
   *
   * @param session        The active NiFi ProcessSession.
   * @param context        The NiFi ProcessContext.
   * @param flowFile       The FlowFile containing the file to process.
   * @param reportFlowFile The FlowFile where the report is to be written.
   * @throws AnalysisException There was a problem performing file analysis.
   */
  protected void analyzeFile(ProcessSession session, ProcessContext context,
                             FlowFile flowFile, FlowFile reportFlowFile)
                             throws AnalysisException
  {
    try (DropoffSession dropoff = getSessionFactory(context).getSession())
    {
      dropoff.connect();

      // make use the FlowFile UUID in hope of avoiding collisions on
      // the analysis host
      String originalFileName = flowFile.getAttribute(CoreAttributes.FILENAME.key());
      String uuid = flowFile.getAttribute(CoreAttributes.UUID.key());
      String extension = FilenameUtils.getExtension(originalFileName);
      String filename = uuid + "." + extension;
      String reportName = filename;
      if (!dropoff.getReportExtension().isEmpty())
      {
        reportName = reportName + dropoff.getReportExtension();
      }
      else
      {
        reportName  = reportName + ".xml";
      }

      putFile(dropoff, session, flowFile, filename);
      getFile(dropoff, session, reportFlowFile, reportName);
      putReportAttributes(dropoff, session, reportFlowFile);
      deleteFile(dropoff, reportName);

      session.getProvenanceReporter().modifyContent(reportFlowFile,
                                                    MODIFIED_REPORT_CONTENT);
      session.putAttribute(reportFlowFile, AnalysisDBWriter.ANALYSIS_SUCCESS,
                           Boolean.TRUE.toString());
    }
    catch (FileTimeoutException fte)
    {
      throw new AnalysisException("The file processing timeout was reached",
                                  fte);
    }
    catch (IOException ioe)
    {
      throw new AnalysisException("An IO error occurred during file analysis",
                                  ioe);
    }
    catch (SessionException se)
    {
      throw new AnalysisException("A session communication error occurred "
                                  + "during file analysis", se);
    }
  }

  @Override
  public void initInternal()
  {}

  /**
   * The framework calls this method when there is work for this processor.
   * Performs analysis of the given FlowFile and places the resulting
   * analysis report within a cloned FlowFile.
   *
   * @param context  Provides a bridge between a Processor and the framework.
   *                 Includes information about how the Processor is currently
   *                 configured and allows the Processor to perform
   *                 Framework-specific tasks.
   * @param session  Provides a mechanism by which FlowFiles can be created,
   *                 destroyed, examined, cloned, and transferred to other
   *                 Processors.
   * @param flowFile The FlowFile to process.
   */
  @Override
  public void onTriggerInternal(final ProcessContext context,
                                final ProcessSession session,
                                FlowFile flowFile)
    throws ProcessException
  {
    // clone the flowFile attributes, but not any of the file content
    // this will be used to hold the analysis report
    FlowFile reportFlowFile = session.clone(flowFile, 0, 0);

    try
    {
      // write analysis engine id (primary key) to flow file for
      // future processors
      this.analyzeEngine = getAnalyzeEngine(context);
      String analyzeEngineId = Long.toString(analyzeEngine.getAnalyzeEngineId());
      getLogger().debug("Found Analyze Engine [id=" + analyzeEngineId + "]");
      reportFlowFile = session.putAttribute(reportFlowFile, SIAFTUtils.ANALYSIS_ENGINE_ID,
                                            analyzeEngineId);
    }
    catch (Exception ex)
    {
      getLogger().error("Failed to attach the AnalyzeEngine Id to the FlowFile", ex);
    }

    try
    {
      analyzeFile(session, context, flowFile, reportFlowFile);
    }
    catch (Exception ex)
    {
      getLogger().error(ExceptionUtils.getFullStackTrace(ex));

      session.putAttribute(reportFlowFile, AnalysisDBWriter.ANALYSIS_SUCCESS,
          Boolean.FALSE.toString());
      session.putAttribute(reportFlowFile, AnalysisDBWriter.ANALYSIS_RESULTS,
          "An error occurred while analyzing the file");
    }

    onSuccess(session, flowFile, reportFlowFile);
  }

  /**
   * This function allows us to only search for the AnalyzeEngine once after
   * this class is instantiated since we do not expect the AnalyzeEngine to
   * ever change during the life of this class.
   * @param context The ProcessContext that is expected to contain the
   *                AnalyzeEngine EngineName as a property called
   *                ANALYSIS_ENGINE_NAME.
   * @return the analyzeEngine associated with this class.
   * @throws PersistenceException Indicates there was a problem occurred
   *                              getting the AnalyzEngine from the database.
   */
  private AnalyzeEngine getAnalyzeEngine(ProcessContext context)
      throws PersistenceException
  {
    if (this.analyzeEngine == null)
    {
      AnalyzeEngineDAL analyzeEngineDAL = new AnalyzeEngineDAL();

      String engineName = context.getProperty(ANALYSIS_ENGINE_NAME_PROPERTY)
          .getValue();
      getLogger().debug("Looking up Analyze Engine [name=" + engineName + "]");

      if (StringUtils.isEmpty(engineName))
      {
        throw new PersistenceException("Analyze Engine name cannot be empty");
      }

      try
      {
        this.analyzeEngine = analyzeEngineDAL.getAnalyzeEngineByEngineName(engineName);
      }
      catch (PersistenceException ex)
      {
        getLogger().error("Failed to get AnalyzeEngine with name " + engineName
                          + ".", ex);
        throw ex;
      }

      getLogger().info("Using engineName " + engineName + " found "
                       + this.analyzeEngine);
    }

    return this.analyzeEngine;
  }

  protected abstract SessionFactory getSessionFactory(ProcessContext context);

  /**
   * Defines the behavior when processing within onTrigger() is successful. This
   * is required due to the static nature of relationships, and therefore
   * transferring of a flowfile must happen within the derived class.
   *
   * @param session        Provides a mechanism by which FlowFiles can be created,
   *                       destroyed, examined, cloned, and transferred to other
   *                       Processors.
   * @param flowFile       The current FlowFile being processed.
   * @param reportFlowFile The FlowFile containing the analysis results.
   */
  protected abstract void onSuccess(final ProcessSession session,
                                    FlowFile flowFile,
                                    FlowFile reportFlowFile);

}
