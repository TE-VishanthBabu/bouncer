package com.zorsecyber.bouncer.api.dal;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.Session;

import com.zorsecyber.bouncer.api.dao.FileSubmission;
import com.zorsecyber.bouncer.api.dao.Task;


public class FileSubmissionDAL {
	private static Object syncLockObj = new Object();
	public FileSubmissionDAL()
	{
	}
	
	public FileSubmission createFileSubmission(long taskId, String filename, String sha256) throws Exception
	{
		EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
	    synchronized (syncLockObj)
	    {
	      try
	      {
	          entityManager.getTransaction().begin();
	          Task task = entityManager.find(Task.class, taskId);
	          
	          FileSubmission fileSubmission = new FileSubmission();
	          fileSubmission.setSha256(sha256);
	          fileSubmission.setTask(task);
	          fileSubmission.setFilename(filename);
	          fileSubmission.setVerdict("failed");
	          if(task.isSanitize()) {
	        	  fileSubmission.setSanitizedVerdict("failed");
	          }
	          
	          entityManager.persist(task);
	          entityManager.flush();
	          entityManager.persist(fileSubmission);
	          entityManager.getTransaction().commit();
	          
	          return fileSubmission;
	      
	      }
	      catch (Exception ex)
	      {
	        if (entityManager != null && entityManager.getTransaction().isActive())
	        {
	          entityManager.getTransaction().rollback();
	        }
	        throw new Exception("Unable to persist FileSubmission sha256 "+sha256+" taskId "+taskId+" : "+ex.getMessage());
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
	
	public static Boolean checkFileSubExists(String sha256, long taskId) throws Exception {
		EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
		Session session = (Session) entityManager.getDelegate();
		synchronized (syncLockObj) {
			Task task;
			try {
				task = TaskDAL.getTask(taskId);
				// Create CriteriaBuilder
				CriteriaBuilder cb = session.getCriteriaBuilder();
				CriteriaQuery<Long> cq = cb.createQuery(Long.class);
				cq.select(cb.count(cq.from(FileSubmission.class)));
				Root<FileSubmission> fileSub = cq.from(FileSubmission.class);
				cq.where(cb.equal(fileSub.<String>get("sha256"), sha256), cb.equal(fileSub.get("task"), task));
				Long num = entityManager.createQuery(cq).getSingleResult();
//				if(num != 0)
//				{
//					System.out.println("Found duplicate submission for sha256 : "+sha256);
//				}
				return num != 0;
				
			} catch (Exception ex) {
				throw new Exception("Unable to get Submission sha256 " + sha256 + " : " + ex.getMessage());
			} finally {
				session.close();
				if (entityManager != null) {
					entityManager.close();
				}
			}
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
}
