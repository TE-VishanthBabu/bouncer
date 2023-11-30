package com.zorsecyber.bouncer.api.dal;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zorsecyber.bouncer.api.dao.FileAttribute;

public class FileAttributeDAL
{
  private static final Logger log = LoggerFactory.getLogger(FileAttributeDAL.class);
  private static Object syncLockObj = new Object();

  public FileAttributeDAL()
  {
  }

  /**
   * Retrieve a FileAttribe by Id.
   *
   * @param id The primary key to search for within the database.
   * @return A FileAttribute object with the provided id.
   */
  public static FileAttribute getFileAttributeById(long id)
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
}
