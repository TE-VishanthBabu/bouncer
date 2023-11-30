package com.nteligen.hq.dhs.siaft.dal;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.hibernate.exception.JDBCConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nteligen.hq.dhs.siaft.dao.FileAttribute;
import com.nteligen.hq.dhs.siaft.exceptions.DatabaseConnectionException;
import com.nteligen.hq.dhs.siaft.exceptions.PersistenceException;
import com.zorsecyber.bouncer.core.dal.FileSubmissionDAL;
import com.zorsecyber.bouncer.core.dao.FileSubmission;
import com.zorsecyber.bouncer.core.dao.SIAnalysis;
import com.nteligen.hq.dhs.siaft.dal.EntityManagerFactorySingleton;

@SuppressWarnings({"checkstyle:abbreviationaswordinname"})
public class FileAttributeDAL
{
  private static final Logger log = LoggerFactory.getLogger(FileAttributeDAL.class);
  private static Object syncLockObj = new Object();
  
  private static long systemUser = 1;

  public FileAttributeDAL()
  {
  }

  /**
   * Create a new FileAttribute.
   *
   * @param fileName The fileName of the new FileAttribute object.
   * @param fileType The mime type of the new FileAttribute object.
   * @param md5 The MD5 hash of the new FileAttribute object.
   * @param originalUuid The UUID of the new FileAttribute object.
   * @return A FileAttribute object that has also been persisted to the database.
   * @throws PersistenceException when an unexpected exception was thrown.
   * @throws DatabaseConnectionException throws when the entitymanager failed to get a database
   *                                     connection. Another attempt at getting the connection could
   *                                     succeed.
   */
  public FileAttribute createNewFileAttribute(String fileName, long submissionId, String fileType,
                                              String md5, String sha256, String originalUuid)
          throws PersistenceException, DatabaseConnectionException
  {
    this.log.debug("Creating new AnalyzeEngine");
    EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();

    synchronized (syncLockObj)
    {
      try
      {
        this.log.debug("Transaction Begin");
        entityManager.getTransaction().begin();

        FileAttribute fileAttribute = new FileAttribute();
        FileSubmission submission = entityManager.find(FileSubmission.class, submissionId);
        fileAttribute.setSubmission(submission);
        fileAttribute.setFileName(fileName);
        fileAttribute.setFileType(fileType);
        fileAttribute.setMd5(md5);
        fileAttribute.setSha256(sha256);
        fileAttribute.setOriginalUuid(originalUuid);

        this.log.debug("Created new FileAttribute : " + fileAttribute);
        entityManager.persist(submission);
        entityManager.flush();
        entityManager.persist(fileAttribute);
        // push the information to the database so we can then get the auto
        // generated ID
        entityManager.flush();
        this.log.debug("Persisted and Flushed FileAttribute : " + fileAttribute);
        entityManager.getTransaction().commit();
        this.log.debug("Transaction committed");

        return fileAttribute;
      }
      catch (JDBCConnectionException ex)
      {
        this.log.error("Database Connection Failure. Failed to commit new FileAttribute "
                + "with filename('" + fileName
                + ") fileType(" + fileType + ") md5(" + md5 + ") and originalUuid("
                + originalUuid + ").");
        throw new DatabaseConnectionException("Failed to commit new FileAttribute with filename('"
                + fileName + ") fileType(" + fileType + ") md5(" + md5 + ") and originalUuid("
                + originalUuid + ")", ex);
      }
      catch (Exception ex)
      {
        this.log.error("Failed to commit new FileAttribute with filename('" + fileName
                + ") fileType(" + fileType + ") md5(" + md5 + ") and originalUuid("
                + originalUuid + ").");

        if (entityManager != null && entityManager.getTransaction().isActive())
        {
          this.log.info("Rolling back transaction for new FileAttribute with filename(" + fileName
                          + ") fileType(" + fileType + ") md5(" + md5 + ") and originalUuid("
                          + originalUuid + ").");
          entityManager.getTransaction().rollback();
        }
        throw new PersistenceException("Failed to commit new FileAttribute with filename('"
                + fileName + ") fileType(" + fileType + ") md5(" + md5 + ") and originalUuid("
                + originalUuid + ")", ex);
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
   *
   * @param id The primary key to search for within the database.
   * @return A FileAttribute object with the provided id.
   */
  public FileAttribute getFileAttributeById(long id)
  {
    EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
    try
    {
      FileAttribute fileAttribute = entityManager.find(FileAttribute.class, id);
      return fileAttribute;
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
   * This function retrieves a count of file by MD5.
   * @param md5 The MD5 to search the FileAttributes table for
   * @return Returns The count of file containing that MD5 if the MD5 is found.
   *                 Zero 0 if the MD5 is not found
   */
  public Long getMatchingMd5Count(String md5)
  {
    EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
    Long result = 0L;
    try
    {
      TypedQuery<Long> query = entityManager.createQuery("SELECT COUNT(f) FROM FileAttribute f "
        + "where f.md5 = '" + md5 + "'", Long.class);
      result = (Long) query.getSingleResult();
    }
    catch (IllegalArgumentException ex)
    {
      log.debug(ex.getMessage());
    }
    finally
    {
      entityManager.close();
    }
    return result;
  }
  
	public static Set<SIAnalysis> getSiAnalyses(EntityManager entityManager, FileAttribute fa) throws Exception {
		try {
			return new HashSet<SIAnalysis>((entityManager.find(FileAttribute.class, fa.getFileAttributeId()).getSIAnalyses()));
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new Exception("Could not get siAnalyses for fileAttributeId "+fa.getFileAttributeId());
		}
	}
}
