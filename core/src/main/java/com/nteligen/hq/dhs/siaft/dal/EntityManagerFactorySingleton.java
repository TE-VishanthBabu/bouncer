package com.nteligen.hq.dhs.siaft.dal;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class EntityManagerFactorySingleton
{
  private static EntityManagerFactorySingleton instance = new EntityManagerFactorySingleton();
  private final EntityManagerFactory entityManagerFactory;

  private EntityManagerFactorySingleton()
  {
    this.entityManagerFactory = Persistence.createEntityManagerFactory("BouncerPU");
  }

  public static EntityManagerFactorySingleton getInstance()
  {
    return instance;
  }

  public EntityManager getEntityManager()
  {
    return this.entityManagerFactory.createEntityManager();
  }

  @Override
  @SuppressWarnings({"checkstyle:nofinalizer"})
  public void finalize()
  {
    if (this.entityManagerFactory != null)
    {
      this.entityManagerFactory.close();
    }
  }
}

