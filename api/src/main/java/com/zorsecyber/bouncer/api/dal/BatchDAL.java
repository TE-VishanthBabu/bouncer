package com.zorsecyber.bouncer.api.dal;

import javax.persistence.EntityManager;

import com.zorsecyber.bouncer.api.dao.Batch;
import com.zorsecyber.bouncer.api.dao.User;


public class BatchDAL {
	private static Object syncLockObj = new Object();
	public static Batch createBatch(long userId, String name) throws Exception {
		EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
		synchronized (syncLockObj) {
			try {
				entityManager.getTransaction().begin();
				User user = entityManager.find(User.class, userId);

				Batch batch = new Batch();
				batch.setUser(user);
				batch.setName(name);

				entityManager.persist(user);
				entityManager.flush();
				entityManager.persist(batch);
				entityManager.flush();
				entityManager.getTransaction().commit();

				return batch;

			} catch (Exception ex) {
				if (entityManager != null && entityManager.getTransaction().isActive()) {
					entityManager.getTransaction().rollback();
				}
				throw new Exception(
						"Unable to persist Batch userId "+userId + " : " + ex.getMessage());
			} finally {
				if (entityManager != null) {
					entityManager.close();
				}
			}
		}
	}
	
	public static Batch getBatch(long batchId) throws Exception {
		EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
		synchronized (syncLockObj) {
			try {
				return entityManager.find(Batch.class, batchId);
			} catch (Exception ex) {
				throw new Exception("Unable to get Batch batchId " + batchId + " : " + ex.getMessage());
			} finally {
				if (entityManager != null) {
					entityManager.close();
				}
			}
		}
	}
	
	public static Boolean deleteBatch(long batchId) throws Exception
	{
		EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
		synchronized (syncLockObj) {
			try {
				entityManager.getTransaction().begin();
				Batch batch = entityManager.find(Batch.class, batchId);
				entityManager.remove(batch);
				entityManager.flush();
				entityManager.getTransaction().commit();
				return true;
			} catch (Exception ex) {
				if (entityManager != null && entityManager.getTransaction().isActive()) {
					entityManager.getTransaction().rollback();
				}
				throw new Exception(ex);
			} finally {
				if (entityManager != null) {
					entityManager.close();
				}
			}
		}
	}
	
	public static Boolean setBatchName(long batchId, String name) throws Exception
	{
		EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
		synchronized (syncLockObj) {
			try {
				entityManager.getTransaction().begin();
				Batch batch = entityManager.find(Batch.class, batchId);
				batch.setName(name);
				entityManager.flush();
				entityManager.getTransaction().commit();
				return true;
			} catch (Exception ex) {
				if (entityManager != null && entityManager.getTransaction().isActive()) {
					entityManager.getTransaction().rollback();
				}
				throw new Exception("Unable to set name for batch "+batchId);
			} finally {
				if (entityManager != null) {
					entityManager.close();
				}
			}
		}
	}

}
