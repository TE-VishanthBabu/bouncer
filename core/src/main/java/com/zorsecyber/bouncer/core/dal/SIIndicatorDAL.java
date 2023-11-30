package com.zorsecyber.bouncer.core.dal;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nteligen.hq.dhs.siaft.dal.EntityManagerFactorySingleton;
import com.nteligen.hq.dhs.siaft.dao.Analysis;
import com.nteligen.hq.dhs.siaft.dao.FileAttribute;
import com.nteligen.hq.dhs.siaft.exceptions.PersistenceException;
import com.zorsecyber.bouncer.core.dao.SIAnalysis;
import com.zorsecyber.bouncer.core.dao.SIIndicator;
import com.zorsecyber.bouncer.core.dependencies.SophosIntelixReport;

public class SIIndicatorDAL {
	private static final Logger log = LoggerFactory.getLogger(SIIndicatorDAL.class);
	private static Object syncLockObj = new Object();
	
	public SIIndicatorDAL()
	{
		
	}
	
	public static SIIndicator createSIIndicator(
			long severity,
			String name,
			String description,
			long siAnalysisId) throws PersistenceException {
		EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
		
		synchronized (syncLockObj) {
			try {
				entityManager.getTransaction().begin();

				SIAnalysis siAnalysis = entityManager.find(SIAnalysis.class, siAnalysisId);

				SIIndicator indicator = new SIIndicator();
				indicator.setSiAnalysis(siAnalysis);
				indicator.setSeverity(severity);
				indicator.setName(name);
				indicator.setDescription(description);

				entityManager.persist(siAnalysis);
				entityManager.flush();
				entityManager.persist(indicator);
				entityManager.getTransaction().commit();

				return indicator;

			} catch (Exception ex) {
				if (entityManager != null && entityManager.getTransaction().isActive()) {
					entityManager.getTransaction().rollback();
				}
				throw new PersistenceException("Unable to create SIIndicator with siAnalysisId=" + 
				siAnalysisId, ex);
			} finally {
				if (entityManager != null) {
					entityManager.close();
				}
			}
		}
	}
	
	public static List<SIIndicator> persistSIIndicators(
			SophosIntelixReport report, long siAnalysisId) throws PersistenceException {
		List<SIIndicator> indicators = new ArrayList<SIIndicator>();
		JSONArray analysisSummary = new JSONArray();
		try {
			analysisSummary = report.json.getJSONArray("analysis_summary");
		} catch(Exception e)
		{
			log.error("Could not get analysis_summary field" + e.getMessage());
		}
		long severity;
		String name;
		String description;
		SIIndicator indicator;
		for (int i=0; i<analysisSummary.length(); i++)
		{
			JSONObject indicatorJson = analysisSummary.getJSONObject(i);
			severity = indicatorJson.getLong("severity");
			name = indicatorJson.getString("name");
			description = indicatorJson.getString("description");
			indicator = SIIndicatorDAL.createSIIndicator(severity, name, description, siAnalysisId);
			indicators.add(indicator);
		}
		return indicators;
	}
	
}
