package com.nteligen.hq.dhs.siaft.dal;

import com.nteligen.hq.dhs.siaft.dao.SanitizeEngine;
import com.nteligen.hq.dhs.siaft.exceptions.DatabaseConnectionException;
import com.nteligen.hq.dhs.siaft.exceptions.PersistenceException;
import org.hibernate.exception.JDBCConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import javax.persistence.EntityManager;


/**
 * This is the data access layer for the SanitizeEngines.
 */
@SuppressWarnings({"checkstyle:abbreviationaswordinname"})
public class SanitizeEngineDAL
{
  private static final Logger log = LoggerFactory.getLogger(SanitizeEngineDAL.class);

  public SanitizeEngineDAL()
  {
  }

  /**
   * This retrieves a SanitizeEngine by ID
   * @param id The primary key of the SanitizeEngine
   * @return Returns the found SanitizeEngine or null if the entity is not found.
   * @throws PersistenceException Throws this exception if any error occurs when finding the
   *                              SantizeEngine.
   * @throws DatabaseConnectionException Throws this exception if the database had a connection
   *                                     error
   */
  public SanitizeEngine getSanitizeEngineById(long id)
          throws PersistenceException, DatabaseConnectionException
  {
    EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
    try
    {
      SanitizeEngine sanitizeEngine = entityManager.find(SanitizeEngine.class, id);
      log.debug("Retieved SanitizeEnginefrom the database : " + sanitizeEngine);
      return sanitizeEngine;
    }
    catch (JDBCConnectionException ex)
    {
      this.log.error("Database Connection Failure. Unable to retrieve SanitizeEngine with id "
              + id + ".");
      throw new DatabaseConnectionException("Failed to retrieve SanitizeEngine with id "
              + id + ".", ex);
    }
    // Data Access Layer fault barrier. We will raise any exceptions from here
    // as DataAccessException
    catch (Exception ex)
    {
      throw new PersistenceException("Failed to find SanitizeEngine with id '"
                                     + id + "'.", ex);
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
    * This retrieves a SanitizeEngine by the engineName.
    * NOTE: This does not enforce uniqueness. If there are multiple results then the first one found
    * is returned.
    * @param engineName The engineName to search the sanitizeEngine for
    * @return Returns the found SanitizeEngine or null if the entity is not found.
    * @throws PersistenceException Throws this exception if any error occurs when finding the
    *                              sanitizeEngine.
    * @throws DatabaseConnectionException Throws this exception if the database had a connection
    *                                     error
    */
  public SanitizeEngine getSanitizeEngineByEngineName(String engineName)
          throws PersistenceException, DatabaseConnectionException
  {
    EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
    try
    {
      List<SanitizeEngine> results = (List<SanitizeEngine>)(entityManager.createQuery(
          "SELECT sanitizeEngine FROM SanitizeEngine sanitizeEngine "
          + "where sanitizeEngine.engineName = :value1")
              .setParameter("value1", engineName).getResultList());
      if (!results.isEmpty())
      {
        // ignores multiple results and returning the first one found
        log.debug("Retrieved SanitizeEngine from the database : " + results.get(0));
        return results.get(0);
      }
      else
      {
        log.debug("No SanitizeEngine results found for EngineName '" + engineName + "'.");
        return null;
      }
    }
    catch (JDBCConnectionException ex)
    {
      this.log.error("Database Connection Failure. Unable to retrieve SanitizeEngine with name "
              + engineName + ".");
      throw new DatabaseConnectionException("Failed to retrieve SanitizeEngine with name "
              + engineName + ".", ex);
    }
    // Data Access Layer fault barrier. We will raise any exceptions from here
    // as DataAccessException
    catch (Exception ex)
    {
      throw new PersistenceException("Failed to fine SanitizeEngine with name '"
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
