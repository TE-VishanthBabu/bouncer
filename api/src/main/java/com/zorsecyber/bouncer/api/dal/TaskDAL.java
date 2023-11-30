package com.zorsecyber.bouncer.api.dal;

import javax.persistence.EntityManager;

import com.zorsecyber.bouncer.api.dao.Batch;
import com.zorsecyber.bouncer.api.dao.Task;
import com.zorsecyber.bouncer.api.lib.status.TaskStatus;


public class TaskDAL {
	private static Object syncLockObj = new Object();

	public TaskDAL() {
	}

	public static Task createTask(long batchId, String source, boolean sanitizeFiles) throws Exception {
		EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
		synchronized (syncLockObj) {
			try {
				entityManager.getTransaction().begin();
				Batch batch = entityManager.find(Batch.class, batchId);

				Task task = new Task();
				task.setBatch(batch);
				task.setSource(source);
				task.setSanitize(sanitizeFiles);
				task.setStatus(TaskStatus.PENDING);

				entityManager.persist(batch);
				entityManager.flush();
				entityManager.persist(task);
				entityManager.getTransaction().commit();
				return task;

			} 
			catch (Exception ex)
			{
				if (entityManager != null && entityManager.getTransaction().isActive()) {
					entityManager.getTransaction().rollback();
				}
				throw new Exception(
						"Unable to persist Task batchId " + batchId + " source " + source + " : " + ex.getMessage());
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

	public static Task getTask(long taskId) throws Exception {
		return TaskDAL.getTask(null, taskId);
	}
	
	public static Task getTask(EntityManager entityManager, long taskId) throws Exception {
		Boolean close = false;
		if(entityManager == null) {
			entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
			close = true;
		}
		synchronized (syncLockObj) {
			try {
				return entityManager.find(Task.class, taskId);
			} catch (Exception ex) {
				throw new Exception("Unable to get Task taskId " + taskId + " : " + ex.getMessage());
			} finally {
				if (close && entityManager != null) {
					entityManager.close();
				}
			}
		}
	}

	public static Boolean setTaskStatus(long taskId, TaskStatus status) {
		EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
		synchronized (syncLockObj) {
			try {
				entityManager.getTransaction().begin();
				Task task = entityManager.find(Task.class, taskId);
				task.setStatus(status);
				entityManager.flush();
				entityManager.getTransaction().commit();
				return true;
			} catch (Exception ex) {
				if (entityManager != null && entityManager.getTransaction().isActive()) {
					entityManager.getTransaction().rollback();
				}
				return false;
//				throw new Exception(
//						"Unable to update Task taskId " + taskId + " status " + status + " : " + ex.getMessage());
			} finally {
				if (entityManager != null) {
					entityManager.close();
				}
			}
		}
	}
	
	public static Boolean setNumberOfFiles(long taskId, int numberOfFiles) throws Exception {
		EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
		synchronized (syncLockObj) {
			try {
				entityManager.getTransaction().begin();
				Task task = entityManager.find(Task.class, taskId);
				task.setNumberOfFiles(numberOfFiles);
				entityManager.flush();
				entityManager.getTransaction().commit();
				return true;
			} catch (Exception ex) {
				if (entityManager != null && entityManager.getTransaction().isActive()) {
					entityManager.getTransaction().rollback();
				}
				throw new Exception(
						"Unable to update Task taskId " + taskId + " numberOfFiles " + numberOfFiles + " : " + ex.getMessage());
			} finally {
				if (entityManager != null) {
					entityManager.close();
				}
			}
		}
	}
	
	public static Boolean setSize(long taskId, float size) throws Exception {
		EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
		synchronized (syncLockObj) {
			try {
				entityManager.getTransaction().begin();
				Task task = entityManager.find(Task.class, taskId);
				task.setSize(size);
				entityManager.flush();
				entityManager.getTransaction().commit();
				return true;
			} catch (Exception ex) {
				if (entityManager != null && entityManager.getTransaction().isActive()) {
					entityManager.getTransaction().rollback();
				}
				throw new Exception(
						"Unable to update Task taskId " + taskId + " while setting size " + size + " : " + ex.getMessage());
			} finally {
				if (entityManager != null) {
					entityManager.close();
				}
			}
		}
	}
	
	public static void setMessage(long taskId, String message) throws Exception {
		EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
		synchronized (syncLockObj) {
			try {
				entityManager.getTransaction().begin();
				Task task = entityManager.find(Task.class, taskId);
				task.setMessage(message);
				entityManager.flush();
				entityManager.getTransaction().commit();
			} catch (Exception ex) {
				if (entityManager != null && entityManager.getTransaction().isActive()) {
					entityManager.getTransaction().rollback();
				}
				throw new Exception(
						"Unable to update Task taskId " + taskId + " while setting message " + message + " : " + ex.getMessage());
			} finally {
				if (entityManager != null) {
					entityManager.close();
				}
			}
		}
	}
	
	public static void setSource(long taskId, String source) throws Exception {
		EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
		synchronized (syncLockObj) {
			try {
				entityManager.getTransaction().begin();
				Task task = entityManager.find(Task.class, taskId);
				task.setSource(source);
				entityManager.flush();
				entityManager.getTransaction().commit();
			} catch (Exception ex) {
				if (entityManager != null && entityManager.getTransaction().isActive()) {
					entityManager.getTransaction().rollback();
				}
				throw new Exception(
						"Unable to update Task taskId " + taskId + " while setting source " + source + " : " + ex.getMessage());
			} finally {
				if (entityManager != null) {
					entityManager.close();
				}
			}
		}
	}
	
	public static int getFilesCompletedByCSM(long taskId) throws Exception
	{
		Task task = TaskDAL.getTask(taskId);
		return task.getClean()+task.getSuspicious()+task.getMalicious();
	}
	
	public static void resetTask(Task task) {
		task.setClean(0);
		task.setSuspicious(0);
		task.setMalicious(0);
		task.setMessage(null);
		task.setStatus(TaskStatus.PENDING);
		task.setNumberOfFiles(0);
		task.setFilesCompleted(0);
	}
}
