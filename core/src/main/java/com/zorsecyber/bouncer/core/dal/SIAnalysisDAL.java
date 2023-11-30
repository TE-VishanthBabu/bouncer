package com.zorsecyber.bouncer.core.dal;

import java.util.Date;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nteligen.hq.dhs.siaft.dal.EntityManagerFactorySingleton;
import com.nteligen.hq.dhs.siaft.dao.Analysis;
import com.nteligen.hq.dhs.siaft.dao.FileAttribute;
import com.nteligen.hq.dhs.siaft.exceptions.PersistenceException;
import com.zorsecyber.bouncer.core.dao.SIAnalysis;

public class SIAnalysisDAL {
	private static final Logger log = LoggerFactory.getLogger(SIAnalysisDAL.class);
	private static Object syncLockObj = new Object();

	public SIAnalysisDAL() {
	}

	  /**
	   * This will create a new SIAnalysis object from the information provided and persist that object to
	   * the database prior to the object being returned to the caller.
	   * NOTE: The newly created SIAnalysis object will have the primary key populated.
	   * @param fileAttributesId The fileAttribute the SIanalysis is associated with.
	   * @param analysisId The analysis the SIanalysis is associated with.
	   * @param jobUUID The jobUUID of the SIanalysis
	   * @param reportSource The data source from which the SIanalysis report was retrieved
	   * @param status The status of the SIanalysus
	   * @param analysisType The SIanalysis type
	   * @param score The SIanalysis score
	   * @param sha256 The sha256 checksum of the analyzed file.
	   * @param staticIndicatorCount The number of static indicators associated with the SIanalysis
	   * @param maliciousActivityCount The number of malicious activity indicators associated with the SIanalysis
	   * @param mimeType The mime type of the analyzed file
	   * @param reportLevel The SIanalysis report level
	   * @param submissionTimestamp The submission timestamp
	   * @return The new SIanalysis object
	   * @throws PersistenceException Throws this exception when there was a problem persisting the
	   *                              entry to the database.
	   */
	public SIAnalysis createSIAnalysis(
			long fileAttributeId,
			long analysisId,
			String jobUUID,
			String reportSource,
			String status,
			String analysisType,
			int score,
			String verdict,
			String sha256,
			Boolean sanitized,
			int staticIndicatorCount,
			int maliciousActivityCount,
			String mimeType,
			int reportLevel,
			Date submissionTimestamp) throws PersistenceException {
		this.log.debug("Creating new SIAnalysis");
		EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
		synchronized (syncLockObj) {
			try {
				this.log.debug("Transaction Begin");
				entityManager.getTransaction().begin();

				FileAttribute fileAttribute = entityManager.find(FileAttribute.class, fileAttributeId);
				this.log.debug("Retrieved FileAttribute : " + fileAttribute);

				SIAnalysis SiAnalysis = new SIAnalysis();
				SiAnalysis.setFileAttribute(fileAttribute);
				Analysis analysis = entityManager.find(Analysis.class, analysisId);
				SiAnalysis.setAnalysis(analysis);
				SiAnalysis.setJobUUID(jobUUID);
				SiAnalysis.setReportSource(reportSource);
				SiAnalysis.setStatus(status);
				SiAnalysis.setAnalysisType(analysisType);
				SiAnalysis.setScore(score);
				SiAnalysis.setVerdict(verdict);
				SiAnalysis.setSha256(sha256);
				SiAnalysis.setSanitized(sanitized);
				SiAnalysis.setStaticIndicatorCount(staticIndicatorCount);
				SiAnalysis.setMaliciousActivityCount(maliciousActivityCount);
				SiAnalysis.setMimeType(mimeType);
				SiAnalysis.setReportLevel(reportLevel);
				SiAnalysis.setSubmissionTimestamp(submissionTimestamp);

				this.log.debug("Created new SIAnalysis : " + SiAnalysis);
				entityManager.persist(fileAttribute);
				entityManager.flush();
				entityManager.persist(SiAnalysis);
				this.log.debug("Persisted and Flushed SIAnalysis : " + SiAnalysis);
				entityManager.getTransaction().commit();
				this.log.debug("Transaction committed");

				return SiAnalysis;

			} catch (Exception ex) {
				if (entityManager != null && entityManager.getTransaction().isActive()) {
					entityManager.getTransaction().rollback();
				}
				throw new PersistenceException("Unable to create SIAnalysis with fileAttributeId=" + fileAttributeId
						+ " analysisId=" + analysisId + " categoryId=", ex);
			} finally {
				if (entityManager != null) {
					entityManager.close();
				}
			}
		}
	}
	
	public static SIAnalysis getSiAnalysis(long siAnalysisId) throws PersistenceException
	{
		EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
		synchronized (syncLockObj) {
			try {
				log.debug("Transaction Begin");
				entityManager.getTransaction().begin();

				return entityManager.find(SIAnalysis.class, siAnalysisId);

			} catch (Exception ex) {
				if (entityManager != null && entityManager.getTransaction().isActive()) {
					entityManager.getTransaction().rollback();
				}
				throw new PersistenceException("Unable to find SIAnalysis with Id=" + siAnalysisId, ex);
			} finally {
				if (entityManager != null) {
					entityManager.close();
				}
			}
		}
	}

	  /**
	   * Gets the analysis verdict of an SIAnalysis for a specific fileAttribute
	   * from the database
	   * @param fileAttributesId The fileAttribute the SIanalysis is associated with.
	   * @return The SIanalysis verdict
	 * @throws Exception 
	   */
	public static String getStaticAnalysisVerdict(Long fileAttributeID, Boolean sanitized) throws Exception {
		EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
		String verdict = "";
		try {
			TypedQuery<String> query = entityManager.createQuery(
					"SELECT verdict from SIAnalysis a " 
							+ "where a.fileAttribute.fileAttributeId = :value1 "
							+ "and a.sanitized = :value2 "
							+ "and a.analysisType = 'static'",
					String.class);
			query.setParameter("value1", fileAttributeID);
			query.setParameter("value2", sanitized);
			verdict = (String) query.getSingleResult();
			return verdict;
		} catch (IllegalArgumentException ex) {
			throw new Exception("Failed to get staticAnalysis verdict for fileAttribute " + fileAttributeID + ": " + ex.getMessage());
		} finally {
			entityManager.close();
		}
	}

	  /**
	   * Computes an SIAnalysis verdict from its score
	   * @param score The SIAnalysis score.
	   * @param analysisType The SIAnalysis type.
	   * @return The SIanalysis verdict.
	   */
	public static String determineAnalysisVerdict(int score, String analysisType) {
		String verdict = new String();
		if (analysisType.equals("dynamic")) {
			if (score == 0) {
				verdict = "malicious";
			} else {
				verdict = "clean";
			}
		} else {
			if (score < 50) {
				if (score < 20) {
					verdict = "malicious";
				} else {
					verdict = "suspicious";
				}
			} else {
				verdict = "clean";
			}
		}
		return verdict;
	}

	  /**
	   * Compute the sum of SIIndicators of an SIAnalysis
	   * @param fileAttributeID The fileAttribute belonging to the SIAnalysis
	   * @return The SIIndicator count.
	   */
	public static int getSIIndicatorCount(Long fileAttributeID) throws Exception {
		EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
		synchronized (syncLockObj) {
			try {
				TypedQuery<Long> query = entityManager.createQuery(
						"select i.staticIndicatorCount as c from siAnalsis i.fileAttribute= :fileAttributeId",
						Long.class);
				return query.getSingleResult().intValue();
			} catch (IllegalArgumentException ex) {
				log.warn(ex.getMessage());
				return 0;
			} finally {
				entityManager.close();
			}
		}
	}
}
