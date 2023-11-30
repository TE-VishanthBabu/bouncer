package com.nteligen.hq.dhs.siaft.dal;

import com.nteligen.hq.dhs.siaft.dao.AnalyzeEngine;
import com.nteligen.hq.dhs.siaft.exceptions.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import javax.persistence.EntityManager;

/**
 * This is the data access layer for the AnalyzeEngines.
 */
@SuppressWarnings({"checkstyle:abbreviationaswordinname"})
public class AnalyzeEngineDAL
{
  private static final Logger log = LoggerFactory.getLogger(AnalyzeEngineDAL.class);

  public AnalyzeEngineDAL()
  {
  }

  /**
   * Retreive an AnalyzeEngine based on Id.
   *
   * @param id The primary key of the AnalyzeEngine to retrieve.
   * @return An AnalyzeEngine object.
   */
  public AnalyzeEngine getAnalyzeEngineById(long id)
  {
    EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
    try
    {
      AnalyzeEngine analyzeEngine = entityManager.find(AnalyzeEngine.class, id);
      return analyzeEngine;
    }
    finally
    {
      if (entityManager != null)
      {
        entityManager.close();
      }
    }
  }

  /**
   * This retrieves a AnalyzeEngine by the engineName.
   * NOTE: This does not enforce uniqueness. If there are multiple results then the first one found
   * is returned.
   * @param engineName The engineName to search the AnalyzeEngine for
   * @return Returns the found AnalyzeEngine or null if the entity is not found.
   * @throws PersistenceException Throws this exception if any error occurs when
   *                              finding the sanitizeEngine.
   */
  public AnalyzeEngine getAnalyzeEngineByEngineName(String engineName) throws PersistenceException
  {
    EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
    try
    {
      List<AnalyzeEngine> results = (List<AnalyzeEngine>)(entityManager.createQuery(
              "SELECT analyzeEngine FROM AnalyzeEngine analyzeEngine "
                      + "where analyzeEngine.engineName = :value1")
              .setParameter("value1", engineName).getResultList());
      if (!results.isEmpty())
      {
        // ignores multiple results and returning the first one found
        log.debug("Retrieved from the database : " + results.get(0));
        return results.get(0);
      }
      else
      {
        log.debug("No AnalyzeEngine results found for EngineName '" + engineName + "'.");
        return null;
      }
    }
    // Data Access Layer fault barrier. We will raise any exceptions from here
    // as DataAccessException
    catch (Exception ex)
    {
      throw new PersistenceException("Failed to fine AnalyzeEngine with name '"
              + engineName + "'.", ex);
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
