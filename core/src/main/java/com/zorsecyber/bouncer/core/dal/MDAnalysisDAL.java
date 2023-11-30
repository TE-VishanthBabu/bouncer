package com.zorsecyber.bouncer.core.dal;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nteligen.hq.dhs.siaft.dal.EntityManagerFactorySingleton;
import com.nteligen.hq.dhs.siaft.dao.Analysis;
import com.nteligen.hq.dhs.siaft.dao.FileAttribute;
import com.nteligen.hq.dhs.siaft.exceptions.PersistenceException;
import com.zorsecyber.bouncer.core.dao.MDAnalysis;

public class MDAnalysisDAL {
	private static final Logger log = LoggerFactory.getLogger(MDAnalysisDAL.class);
	private static Object syncLockObj = new Object();

	public MDAnalysisDAL() {
	}

	/**
	   * This will create a new MDAnalysis object from the information provided and persist that object to
	   * the database prior to the object being returned to the caller.
	   * NOTE: The newly created SIAnalysis object will have the primary key populated.
	   * @param fileAttributesId The fileAttribute the SIanalysis is associated with.
	   * @param analysisId The analysis the SIanalysis is associated with.
	   * @param dataId	The dataId associated with the analysis
	   * @param fileId	The fileId associated with the analysis
	   * @param sha256 The sha256 checksum of the analyzed file.
	   * @return The new MDanalysis object
	   * @throws PersistenceException Throws this exception when there was a problem persisting the
	   *                              entry to the database.
	   */
	public MDAnalysis createMDAnalysis(
			long fileAttributeId,
			long analysisId,
			String dataId,
			String fileId,
			String sha256) throws PersistenceException {
		this.log.debug("Creating new MDAnalysis");
		EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
		synchronized (syncLockObj) {
			try {
				MDAnalysisDAL.log.debug("Transaction Begin");
				entityManager.getTransaction().begin();
				FileAttribute fileAttribute = entityManager.find(FileAttribute.class, fileAttributeId);
				this.log.debug("Retrieved FileAttribute : " + fileAttribute);
				MDAnalysis MdAnalysis = new MDAnalysis();
				MdAnalysis.setFileAttribute(fileAttribute);
				Analysis analysis = entityManager.find(Analysis.class, analysisId);
				MdAnalysis.setAnalysis(analysis);
				MdAnalysis.setDataId(dataId);
				MdAnalysis.setFileId(fileId);
				MdAnalysis.setSha256(sha256);

				MDAnalysisDAL.log.debug("Created new MDAnalysis : " + MdAnalysis);
				entityManager.persist(fileAttribute);
				entityManager.flush();
				entityManager.persist(MdAnalysis);
				MDAnalysisDAL.log.debug("Persisted and Flushed MDAnalysis : " + MdAnalysis);
				entityManager.getTransaction().commit();
				MDAnalysisDAL.log.debug("Transaction committed");

				return MdAnalysis;

			} catch (Exception ex) {
				if (entityManager != null && entityManager.getTransaction().isActive()) {
					entityManager.getTransaction().rollback();
				}
				throw new PersistenceException("Unable to create MDAnalysis with fileAttributeId=" + fileAttributeId
						+ " analysisId=" + analysisId + " categoryId=", ex);
			} finally {
				if (entityManager != null) {
					entityManager.close();
				}
			}
		}
	}
}
