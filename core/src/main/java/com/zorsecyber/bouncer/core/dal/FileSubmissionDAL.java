package com.zorsecyber.bouncer.core.dal;

import java.util.List;

import javax.persistence.EntityManager;

import com.nteligen.hq.dhs.siaft.dal.EntityManagerFactorySingleton;
import com.nteligen.hq.dhs.siaft.dao.FileAttribute;
import com.nteligen.hq.dhs.siaft.exceptions.DatabaseConnectionException;
import com.nteligen.hq.dhs.siaft.exceptions.PersistenceException;
import com.zorsecyber.bouncer.core.dao.FileSubmission;

public class FileSubmissionDAL {
	private static Object syncLockObj = new Object();
	
	public FileSubmissionDAL()
	{
	}
	
	  /**
	   * This method sets the success column for a specific FileSubmissionId.
	   * @param submissionId The submissionId of the file.
	   * @param success whether the submission should be set to succeeded or failed.
	   * @return Boolean yes if successful
	   * @throws PersistenceException Throws this exception when there was a problem persisting the
	   *                              entry to the database.
	   * @throws DatabaseConnectionException There was a problem connecting to the database.
	   */
	public static Boolean SetFileSubmissionSuccess(long submissionId, boolean success){
			EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
			synchronized (syncLockObj) {
//				try {
					entityManager.getTransaction().begin();
					FileSubmission submission = entityManager.find(FileSubmission.class, submissionId);
					submission.setSuccess(true);
					entityManager.flush();
					entityManager.getTransaction().commit();
					return true;
//				} catch (Exception ex) {
//					
//					ex.printStackTrace();
//					if (entityManager != null && entityManager.getTransaction().isActive()) {
//						entityManager.getTransaction().rollback();
//					}
//					throw new Exception("Unable to get FileSubmission submissionId " + submissionId + " : " + ex.getMessage());
//				} finally {
//					if (entityManager != null) {
//						entityManager.close();
//					}
//				}
			}
		}
	
	public static FileSubmission getFileSubmission(long submissionId) throws Exception {
		EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
		synchronized (syncLockObj) {
			try {
				return entityManager.find(FileSubmission.class, submissionId);
			} catch (Exception ex) {
				throw new Exception("Unable to get FileSubmission submissionId " + submissionId + " : " + ex.getMessage());
			} finally {
				if (entityManager != null) {
					entityManager.close();
				}
			}
		}
	}
	
	public static Boolean setVerdict(long submissionId, String verdict) throws Exception {
		EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
		synchronized (syncLockObj) {
			try {
				entityManager.getTransaction().begin();
				FileSubmission submission = entityManager.find(FileSubmission.class, submissionId);
				submission.setVerdict(verdict);
				entityManager.flush();
				entityManager.getTransaction().commit();
				return true;
			} catch (Exception ex) {
				
				ex.printStackTrace();
				if (entityManager != null && entityManager.getTransaction().isActive()) {
					entityManager.getTransaction().rollback();
				}
				throw new Exception("Unable to get FileSubmission submissionId " + submissionId + " : " + ex.getMessage());
			} finally {
				if (entityManager != null) {
					entityManager.close();
				}
			}
		}
	}
	
	public static Boolean setSanitizedVerdict(long submissionId, String verdict) throws Exception {
		EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
		synchronized (syncLockObj) {
			try {
				entityManager.getTransaction().begin();
				FileSubmission submission = entityManager.find(FileSubmission.class, submissionId);
				submission.setSanitizedVerdict(verdict);
				entityManager.flush();
				entityManager.getTransaction().commit();
				return true;
			} catch (Exception ex) {
				
				ex.printStackTrace();
				if (entityManager != null && entityManager.getTransaction().isActive()) {
					entityManager.getTransaction().rollback();
				}
				throw new Exception("Unable to get FileSubmission submissionId " + submissionId + " : " + ex.getMessage());
			} finally {
				if (entityManager != null) {
					entityManager.close();
				}
			}
		}
	}
	
	public static void setSanitizeStatus(long submissionId, String sanitizeStatus) throws Exception {
		EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
		synchronized (syncLockObj) {
			try {
				entityManager.getTransaction().begin();
				FileSubmission fileSubmission = entityManager.find(FileSubmission.class, submissionId);
				fileSubmission.setSanitizeStatus(sanitizeStatus);
				entityManager.flush();
				entityManager.getTransaction().commit();
			} catch (Exception ex) {
				if (entityManager != null && entityManager.getTransaction().isActive()) {
					entityManager.getTransaction().rollback();
				}
				} finally {
				if (entityManager != null) {
					entityManager.close();
				}
			}
		}
	}
	
	public static FileAttribute getFileAttribute(EntityManager entityManager, FileSubmission fs) throws Exception {
		try {
			return ((List<FileAttribute>) (entityManager.find(FileSubmission.class, fs.getSubmissionId()).getFileAttributes())).get(0);
		} catch (Exception ex) {
			throw new Exception("Could not get fileAttribure for submissionId "+fs.getSubmissionId());
		}
	}

}
