package com.nteligen.hq.dhs.siaft.dal;

import com.nteligen.hq.dhs.siaft.dao.Analysis;
import com.nteligen.hq.dhs.siaft.dao.AnalyzeEngine;
import com.nteligen.hq.dhs.siaft.dao.FileAttribute;
import com.nteligen.hq.dhs.siaft.dao.SanitizeEngine;
import com.nteligen.hq.dhs.siaft.exceptions.DatabaseConnectionException;
import com.nteligen.hq.dhs.siaft.exceptions.EntityNotFoundException;
import com.nteligen.hq.dhs.siaft.exceptions.PersistenceException;
import org.hibernate.exception.JDBCConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;

@SuppressWarnings({"checkstyle:abbreviationaswordinname"})
public class AnalysisDAL
{
  private static final Logger log = LoggerFactory.getLogger(AnalysisDAL.class);
  private static Object syncLockObj = new Object();

  public AnalysisDAL()
  {
  }

  /**
   * This will create a new Analysis object from the information provided and persist that object to
   * the database prior to the object being returned to the caller.
   * NOTE: The newly created Analysis object will have the primary key populated.
   * @param fileAttributesId The fileAttribute the analysis is associated with.
   * @param analysisEngineId The analysis Engine the analysis is associated with.
   * @param success Whether the analysis was successful or not.
   * @param results The results from the analysis.
   * @return The new analysis object
   * @throws PersistenceException Throws this exception when there was a problem persisting the
   *                              entry to the database.
   * @throws DatabaseConnectionException There was a problem connecting to the database.
   */
  public Analysis createAnalysis(long fileAttributesId,
                                 long analysisEngineId,
                                 boolean success,
                                 String results)
          throws PersistenceException, DatabaseConnectionException
  {
    return createAnalysis(fileAttributesId, analysisEngineId, null, success, results);
  }

  /**
   * This will create a new Analysis object from the information provided and persist that object to
   * the database prior to the object being returned to the caller.
   * NOTE: The newly created Analysis object will have the primary key populated.
   * @param fileAttributesId The fileAttribute the analysis is associated with.
   * @param analysisEngineId The analysis Engine the analysis is associated with.
   * @param sanitizeEngineId The sanitize Engine the analysis is associated with. This can be null.
   * @param success Whether the analysis was successful or not.
   * @param results The results from the analysis.
   * @return The new analysis object
   * @throws PersistenceException Throws this exception when there was a problem persisting the
   *                              entry to the database.
   * @throws DatabaseConnectionException There was a problem connecting to the database.
   */
  public Analysis createAnalysis(long fileAttributesId,
                                 long analysisEngineId,
                                 Long sanitizeEngineId,
                                 boolean success,
                                 String results)
          throws PersistenceException, DatabaseConnectionException
  {
    synchronized (syncLockObj)
    {
      EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
      try
      {
        this.log.debug("Transaction Begin");
        entityManager.getTransaction().begin();
        FileAttribute fileAttribute = entityManager.find(FileAttribute.class, fileAttributesId);
        if (fileAttribute == null)
        {
          throw new EntityNotFoundException("FileAttributes with id " + fileAttributesId
                  + " was not found in the database");
        }
        this.log.debug("Extracted from the database FileAttribute " + fileAttribute);
        Analysis analysis = null;
        AnalyzeEngine analyzeEngine = entityManager.find(AnalyzeEngine.class, analysisEngineId);
        if (analyzeEngine == null)
        {
          throw new EntityNotFoundException("AnalyzeEngine with id " + analysisEngineId
                 + " was not found in the database");
        }
        if (sanitizeEngineId != null)
        {
          SanitizeEngine sanitizeEngine =
            entityManager.find(SanitizeEngine.class, sanitizeEngineId);
          if (sanitizeEngine == null)
          {
            throw new EntityNotFoundException("SanitizeEngine with id " + sanitizeEngineId
                    + " was not found in the database");
          }
          this.log.debug("Extracted from the database SanitizeEngine " + sanitizeEngine);
          analysis = new Analysis(fileAttribute, analyzeEngine, sanitizeEngine, success);
        }
        else
        {
          this.log.debug("Creating a new Analysis without an associated SanitizationEngine");
          analysis = new Analysis(fileAttribute, analyzeEngine, success);
        }
        analysis.setResults(results);
        this.log.debug("Persisting to the database Analysis " + analysis);
        fileAttribute.getAnalyses().add(analysis);
        entityManager.persist(fileAttribute);
        entityManager.flush();
        this.log.debug("Flushed to the database FileAttribute " + fileAttribute);
        this.log.info("Flushed to the database Analysis " + analysis);
        entityManager.getTransaction().commit();
        this.log.debug("Transaction committed");
        return analysis;
      }
      catch (JDBCConnectionException ex)
      {
        this.log.error("Database Connection Failure. Failed to create and persist new Analysis "
                + "for fileAttribute("
                + fileAttributesId + ") and SanitizeEngine(" + sanitizeEngineId + ")");
        throw new DatabaseConnectionException("Failed to commit new Analysis for fileAttribute("
                + fileAttributesId + ") and SanitizeEngine(" + sanitizeEngineId + ")", ex);
      }
      catch (Exception ex) //fault barrier so we can control the exceptions flowing up the chain.
      {
        String errMsg = "Failed to create and persist new Analysis for fileAttribute("
                + fileAttributesId + ") and SanitizeEngine(" + sanitizeEngineId + ")";
        this.log.error(errMsg);
        throw new PersistenceException(errMsg, ex);
      }
      finally
      {
        if (entityManager != null)
        {
          entityManager.close();
        }
      }
    }
  }
}
