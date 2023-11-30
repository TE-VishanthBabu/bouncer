package com.zorsecyber.bouncer.core.dal;

import java.util.Date;

import javax.persistence.EntityManager;

import com.nteligen.hq.dhs.siaft.dal.EntityManagerFactorySingleton;
import com.zorsecyber.bouncer.core.dao.FileSubmission;
import com.zorsecyber.bouncer.core.dao.SIReputation;

public class SIReputationDAL {
	private static Object syncLockObj = new Object();

	public static SIReputation createSIReputation(long submissionId, Date firstSeen, Date lastSeen, String prevalence, int score, String scoreString) throws Exception {
		EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
		synchronized (syncLockObj) {
			try {
				entityManager.getTransaction().begin();
				SIReputation reputation = new SIReputation();
				FileSubmission fs = entityManager.find(FileSubmission.class, submissionId);
				reputation.setSubmission(fs);
				reputation.setFirstSeen(firstSeen);
				reputation.setLastSeen(lastSeen);
				reputation.setPrevalence(prevalence);
				reputation.setScore(score);
				reputation.setScoreString(scoreString);
				entityManager.persist(fs);
				entityManager.flush();
				entityManager.persist(reputation);
				entityManager.getTransaction().commit();
				return reputation;
			} catch (Exception ex) {
				
				ex.printStackTrace();
				if (entityManager != null && entityManager.getTransaction().isActive()) {
					entityManager.getTransaction().rollback();
				}
				throw new Exception("Unable to persist SIReputation for submission " + submissionId + " : " + ex.getMessage());
			} finally {
				if (entityManager != null) {
					entityManager.close();
				}
			}
		}
	}
	
}
