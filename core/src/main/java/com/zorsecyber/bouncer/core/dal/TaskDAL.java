package com.zorsecyber.bouncer.core.dal;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import com.nteligen.hq.dhs.siaft.dal.EntityManagerFactorySingleton;
import com.zorsecyber.bouncer.core.dao.Task;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TaskDAL {
	private static Object syncLockObj = new Object();
	private static final String incrementCSMQuery = "UPDATE Task t set t.csm = t.csm + 1 WHERE t.taskId = :taskId";

	public TaskDAL() {
	}

	public static Task getTask(long taskId) throws Exception {
		EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
		synchronized (syncLockObj) {
			try {
				return entityManager.find(Task.class, taskId);
			} catch (Exception ex) {
				throw new Exception("Unable to get Task taskId " + taskId + " : " + ex.getMessage());
			} finally {
				if (entityManager != null) {
					entityManager.close();
				}
			}
		}
	}

	public static Boolean incrementCompletedFiles(long taskId) throws Exception {
		EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
		synchronized (syncLockObj) {
			try {
				entityManager.getTransaction().begin();
				Task task = entityManager.find(Task.class, taskId);
				task.setCompletedFiles(task.getCompletedFiles() + 1);
				entityManager.flush();
				entityManager.getTransaction().commit();
				return true;
			} catch (Exception ex) {
				if (entityManager != null && entityManager.getTransaction().isActive()) {
					entityManager.getTransaction().rollback();
				}
				throw new Exception(
						"Unable to update Task taskId " + taskId + " : " + ex.getMessage());
			} finally {
				if (entityManager != null) {
					entityManager.close();
				}
			}
		}
	}

	public static void incrementCSM(long taskId, String verdict) throws Exception {
		EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
		synchronized (syncLockObj) {
			try {
				entityManager.getTransaction().begin();
				String qString = incrementCSMQuery.replace("csm", verdict);
				log.trace("CSM query string: "+qString);
				Query q = entityManager.createQuery(qString);
				q.setParameter("taskId", taskId);
				q.executeUpdate();
				entityManager.flush();
				entityManager.getTransaction().commit();
			} catch (Exception ex) {
				if (entityManager != null && entityManager.getTransaction().isActive()) {
					entityManager.getTransaction().rollback();
				}
				ex.printStackTrace();
			throw new Exception(
					"Unable to update Task taskId " + taskId + " increment CSM with verdict " + verdict + " : " + ex.getMessage());
			} finally {
				if (entityManager != null) {
					entityManager.close();
				}
			}
		}
	}
}
