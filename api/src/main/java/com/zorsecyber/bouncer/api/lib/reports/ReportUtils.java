package com.zorsecyber.bouncer.api.lib.reports;

import java.util.List;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zorsecyber.bouncer.api.dal.EntityManagerFactorySingleton;
import com.zorsecyber.bouncer.api.dao.StaticDynamicReport;
import com.zorsecyber.bouncer.api.dao.reports.ByFileTypeReportRow;



public class ReportUtils {
	private static final Logger log = LoggerFactory.getLogger(ReportUtils.class);
	private static Object syncLockObj = new Object();

	public ReportUtils() {
	}

	public static JSONObject byFileTypeReport(long batchId, String q) throws Exception {
		EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
		synchronized (syncLockObj) {
			try {
				entityManager.getTransaction().begin();
				JSONObject report = new JSONObject();
				report.put("n", "n");
				
				Query baseQuery = entityManager.createQuery(
						q
//								"select t"
//								+" FROM Task t"
//								+" INNER JOIN FileSubmission fs"
//								+" ON fa.FK_Task_ID"
//								+" WHERE t.batch= :batchId"
						);

//				q2.setParameter("batchId",batchId);
				final List<Object[]> l = baseQuery.getResultList();
				for (Object s : l)
				{
					System.out.println(s.toString());
				}
				
				Stream<ByFileTypeReportRow> resultStream = baseQuery.getResultStream();
//				List<Object> l = query.getResultList();
				resultStream.forEach
				(
				row->
					{
						System.out.println(row.getSource());
					}
				);
				
//				String q = "SELECT DISTINCT FileType from FileSubmissions "
//						+ "INNER JOIN FileAttributes FA on FileSubmissions.sha256 = FA.sha256 "
//						+ "WHERE FK_Task_ID= :taskId AND "
//						+ "FileType is not null;";
//				Query query = entityManager.createNativeQuery(q);
//				query.setParameter("taskId", taskId);
//				List<Object> fileTypes = query.getResultList();
//				for (Object fileType : fileTypes)
//				{
//					if (fileType != null)
//					{
//						report.append(fileType.toString(), ReportUtils.byFileTypeSubReport(fileType.toString(), entityManager, taskId).toJson());
//					}
//				}
//				
//				q = "SELECT count(FK_FileAttributeID) "
//						+ "FROM FileSubmissions "
//						+ "INNER JOIN FileAttributes FA on FileSubmissions.sha256 = FA.sha256 "
//						+ "INNER JOIN Intellix I on FA.PK_FileAttributes_ID = I.FK_FileAttributeID "
//						+ "where FK_Task_ID= :taskId AND analysis_type='static';";
//				query = entityManager.createNativeQuery(q);
//				query.setParameter("taskId", taskId);
//				int numTotalFiles = ((Number) query.getSingleResult()).intValue();
//				q = "SELECT count(FK_FileAttributeID) "
//						+ "FROM FileSubmissions "
//						+ "INNER JOIN FileAttributes FA on FileSubmissions.sha256 = FA.sha256 "
//						+ "INNER JOIN Intellix I on FA.PK_FileAttributes_ID = I.FK_FileAttributeID "
//						+ "where FK_Task_ID= :taskId AND FileType is not null and analysis_type='dynamic';";
//				query = entityManager.createNativeQuery(q);
//				query.setParameter("taskId", taskId);
//				int numTotalDynamic = ((Number) query.getSingleResult()).intValue();
//				q = "SELECT count(FK_FileAttributeID) "
//						+ "FROM FileSubmissions "
//						+ "INNER JOIN FileAttributes FA on FileSubmissions.sha256 = FA.sha256 "
//						+ "INNER JOIN Intellix I on FA.PK_FileAttributes_ID = I.FK_FileAttributeID "
//						+ "WHERE FK_Task_ID= :taskId AND analysis_type='dynamic' and verdict='malicious';";
//				query = entityManager.createNativeQuery(q);
//				query.setParameter("taskId", taskId);
//				int numTotalMalicious = ((Number) query.getSingleResult()).intValue();
//				
//				JSONObject total = new JSONObject();
//				total.put("numFiles", numTotalFiles);
//				total.put("numSuspicious", numTotalDynamic-numTotalMalicious);
//				total.put("numMalicious", numTotalMalicious);
//				report.put("total", total);
				
				return report;
				
			} catch (Exception ex) {
				if (entityManager != null && entityManager.getTransaction().isActive()) {
					entityManager.getTransaction().rollback();
				}
				ex.printStackTrace();
				return new JSONObject("failed");
			} finally {
				if (entityManager != null) {
					entityManager.close();
				}
			}
		}
	}
	
	private static StaticDynamicReport byFileTypeSubReport(String fileType, EntityManager entityManager, Long taskId)
	{
		StaticDynamicReport report = new StaticDynamicReport();
		
		String q = "SELECT count(FK_FileAttributeID) "
				+ "FROM FileSubmissions "
				+ "INNER JOIN FileAttributes FA on FileSubmissions.sha256 = FA.sha256 "
				+ "INNER JOIN Intellix I on FA.PK_FileAttributes_ID = I.FK_FileAttributeID "
				+ "where FK_Task_ID= :taskId AND FileType is not null and analysis_type='static' and verdict='suspicious' and FileType= :fileType ;";
		Query query = entityManager.createNativeQuery(q);
		query.setParameter("taskId", taskId);
		query.setParameter("fileType", fileType);
		int result = ((Number) query.getSingleResult()).intValue();
		int numSuspiciousStatic = result;
		
		q = "SELECT count(FK_FileAttributeID) "
				+ "FROM FileSubmissions "
				+ "INNER JOIN FileAttributes FA on FileSubmissions.sha256 = FA.sha256 "
				+ "INNER JOIN Intellix I on FA.PK_FileAttributes_ID = I.FK_FileAttributeID "
				+ "where FK_Task_ID= :taskId AND FileType is not null and analysis_type='static' and verdict='malicious' and FileType= :fileType ;";
		query = entityManager.createNativeQuery(q);
		query.setParameter("taskId", taskId);
		query.setParameter("fileType", fileType);
		result = ((Number) query.getSingleResult()).intValue();
		int numMaliciousStatic = result;
		
		q = "SELECT count(FK_FileAttributeID) "
				+ "FROM FileSubmissions "
				+ "INNER JOIN FileAttributes FA on FileSubmissions.sha256 = FA.sha256 "
				+ "INNER JOIN Intellix I on FA.PK_FileAttributes_ID = I.FK_FileAttributeID "
				+ "WHERE FK_Task_ID= :taskId AND FileType is not null and FileType= :fileType ;";
		query = entityManager.createNativeQuery(q);
		query.setParameter("taskId", taskId);
		query.setParameter("fileType", fileType);
		result = ((Number) query.getSingleResult()).intValue();
		int numFilesTotal = result;
		
		q = "SELECT count(FK_FileAttributeID) "
				+ "FROM FileSubmissions "
				+ "INNER JOIN FileAttributes FA on FileSubmissions.sha256 = FA.sha256 "
				+ "INNER JOIN Intellix I on FA.PK_FileAttributes_ID = I.FK_FileAttributeID "
				+ "where FK_Task_ID= :taskId AND FileType is not null and analysis_type='dynamic' and FileType= :fileType ;";
		query = entityManager.createNativeQuery(q);
		query.setParameter("taskId", taskId);
		query.setParameter("fileType", fileType);
		result = ((Number) query.getSingleResult()).intValue();
		int numFilesDynamic = result;
		
		q = "SELECT count(FK_FileAttributeID) "
				+ "FROM FileSubmissions "
				+ "INNER JOIN FileAttributes FA on FileSubmissions.sha256 = FA.sha256 "
				+ "INNER JOIN Intellix I on FA.PK_FileAttributes_ID = I.FK_FileAttributeID "
				+ "where FK_Task_ID= :taskId AND FileType is not null and analysis_type='dynamic' and verdict='malicious' and FileType= :fileType ;";
		query = entityManager.createNativeQuery(q);
		query.setParameter("taskId", taskId);
		query.setParameter("fileType", fileType);
		result = ((Number) query.getSingleResult()).intValue();
		int numMaliciousDynamic = result;

		report.numFilesStatic = numFilesTotal;
		report.numSuspiciousStatic = numSuspiciousStatic;
		report.numMaliciousStatic = numMaliciousStatic;
		report.numFileDynamic = numFilesDynamic;
		report.numMaliciousDynamic = numMaliciousDynamic;
		return report;
	}
}
