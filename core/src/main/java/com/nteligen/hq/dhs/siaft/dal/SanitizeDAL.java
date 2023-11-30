package com.nteligen.hq.dhs.siaft.dal;

import com.nteligen.hq.dhs.siaft.dao.FileAttribute;
import com.nteligen.hq.dhs.siaft.dao.Sanitize;
import com.nteligen.hq.dhs.siaft.dao.SanitizeEngine;
import com.nteligen.hq.dhs.siaft.exceptions.DatabaseConnectionException;
import com.nteligen.hq.dhs.siaft.exceptions.EntityNotFoundException;
import org.hibernate.exception.JDBCConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

@SuppressWarnings({"checkstyle:abbreviationaswordinname"})
public class SanitizeDAL
{
  private static final Logger log = LoggerFactory.getLogger(SanitizeDAL.class);
  private static Object syncLockObj = new Object();

  /**
   * Creates this DAL.
   */
  public SanitizeDAL()
  {
  }

  /**
   * Creates a new sanitizer and persists to the database.
   * @param fileAttributeId the associated file attribute
   * @param engineId the engine ID
   * @param result the result
   * @param md5 the md5 for the file
   * @param fileType the file type of the file
   * @return the newly created sanitizer
   * @throws DatabaseConnectionException There was a problem connecting to the database.
   */
  public Sanitize createNewSanitize(long fileAttributeId, long engineId, String result,
    String md5, String fileType) throws DatabaseConnectionException
  {
    this.log.debug("Creating new Sanitize record");
    EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();

    synchronized (syncLockObj)
    {
      try
      {
        this.log.debug("Transaction Begin");
        entityManager.getTransaction().begin();

        FileAttribute fileAttribute = entityManager.find(FileAttribute.class,
          fileAttributeId);
        if (null == fileAttribute)
        {
          throw new EntityNotFoundException("FileAttribute with id [" + fileAttributeId
          + "] was not found in the database");
        }
        log.debug("Retrieved FileAttribute : " + fileAttribute);

        SanitizeEngine sanitizeEngine = entityManager.find(SanitizeEngine.class,
           engineId);
        if (null == sanitizeEngine)
        {
          throw new EntityNotFoundException("Sanitize Engine with id [" + engineId
          + "] was not found in the database");
        }
        log.debug("Retrieved Sanitize Engine : " + sanitizeEngine);

        Sanitize sanitize = new Sanitize();
        sanitize.setFileAttribute(fileAttribute);
        sanitize.setSanitizeEngine(sanitizeEngine);
        sanitize.setResults(result);
        sanitize.setFileType(fileType);
        sanitize.setMd5(md5);

        fileAttribute.getSanitizes().add(sanitize);

        this.log.debug("Created new Sanitize record: " + sanitize);
        entityManager.persist(fileAttribute);
        // push the information to the database so we can then get
        // the auto generated ID
        entityManager.flush();
        this.log.debug("Persisted and Flushed Sanitze : " + sanitize);
        entityManager.getTransaction().commit();
        this.log.debug("Transaction committed");

        return sanitize;
      }
      catch (JDBCConnectionException ex)
      {
        this.log.error("Database Connection Failure. Failed to create and persist new Sanitize "
                + "for fileAttribute(" + fileAttributeId
                + ") and engineId(" + engineId + ")"
                + ") and result(" + result + ")"
                + ") and md5(" + md5 + ")"
                + ") and fileType(" + fileType + ")");
        throw new DatabaseConnectionException("Failed to commit new Sanitize"
                + "for fileAttribute(" + fileAttributeId
                + ") and engineId(" + engineId + ")"
                + ") and result(" + result + ")"
                + ") and md5(" + md5 + ")"
                + ") and fileType(" + fileType + ")", ex);
      }
      catch (Exception ex)
      {
        if (entityManager != null && entityManager.getTransaction().isActive())
        {
          entityManager.getTransaction().rollback();
        }
        throw new PersistenceException("Failed to create Sanitize.", ex);
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

  /**
   * Retrieve a FileAttribe by Id.
   * @param id The Sanitize object id to search for.
   * @return A Sanitize object with the given id.
   */
  public Sanitize getSanitizeById(long id)
  {
    EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
    try
    {
      Sanitize sanitize = entityManager.find(Sanitize.class, id);
      return sanitize;
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
