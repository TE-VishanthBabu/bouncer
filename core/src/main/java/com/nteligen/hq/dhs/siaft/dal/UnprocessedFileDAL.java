package com.nteligen.hq.dhs.siaft.dal;

import com.nteligen.hq.dhs.siaft.dao.FileAttribute;
import com.nteligen.hq.dhs.siaft.dao.UnprocessedFile;
import com.nteligen.hq.dhs.siaft.exceptions.DatabaseConnectionException;
import com.nteligen.hq.dhs.siaft.exceptions.EntityNotFoundException;
import com.nteligen.hq.dhs.siaft.exceptions.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;

@SuppressWarnings({"checkstyle:abbreviationaswordinname"})
public class UnprocessedFileDAL
{
  private static final Logger log = LoggerFactory.getLogger(UnprocessedFileDAL.class);
  private static Object syncLockObj = new Object();

  public UnprocessedFileDAL()
  {
  }

  /**
   * This will create a new UnprocessedFile object from the information provided and
   * persist that object to the database prior to the object being returned to the caller.
   * NOTE: The newly created UnprocessedFile object will have the primary key populated.
   *
   * @param fileAttributesId The fileAttribute the analysis is associated with.
   * @param unprocessed      Whether the file was unprocessed or not.
   * @return The new UnprocessedFile object
   * @throws PersistenceException Throws this exception when there was a problem persisting the
   *                               entry to the database.
   */
  public UnprocessedFile createUnprocessedFile(long fileAttributesId,
    boolean unprocessed, String unprocessedReason)
    throws PersistenceException, DatabaseConnectionException
  {
    EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
    synchronized (syncLockObj)
    {
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
        UnprocessedFile unprocessedFile = new UnprocessedFile(fileAttribute, unprocessed,
                                                              unprocessedReason);
        fileAttribute.getUnprocessedFiles().add(unprocessedFile);
        this.log.debug("Persisting to the database UnprocessedFile " + unprocessedFile);
        entityManager.persist(fileAttribute); //this will persist the unprocessedFile as well
        entityManager.flush();
        this.log.debug("Flushed to the database FileAttribute " + fileAttribute);
        this.log.info("Flushed to the database UnprocessedFile " + unprocessedFile);
        entityManager.getTransaction().commit();
        this.log.debug("Transaction committed");
        return unprocessedFile;
      }
      catch (Exception ex) //fault barrier so we can control the exceptions flowing up the chain.
      {
        String errMsg = "Failed to create and persist new UnprocessedFile for fileAttribute("
                + fileAttributesId + ") and unprocessed(" + unprocessed + ") and "
                + "unprocessedReason(" + unprocessedReason + ")";
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
