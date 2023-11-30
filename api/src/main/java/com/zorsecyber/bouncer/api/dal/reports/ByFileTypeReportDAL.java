package com.zorsecyber.bouncer.api.dal.reports;
//package com.zorsecyber.hauberkDetectionApi.dal.reports;
//
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//
//import javax.persistence.EntityManager;
//import javax.persistence.Query;
//
//import org.json.JSONObject;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import com.zorsecyber.hauberkDetectionApi.dal.EntityManagerFactorySingleton;
//import com.zorsecyber.hauberkDetectionApi.dao.Batch;
//import com.zorsecyber.hauberkDetectionApi.dao.reports.ByFileTypeReportRow;
//import com.zorsecyber.hauberkDetectionApi.lib.reports.ProcedurallyGeneratedReport;
//import com.zorsecyber.hauberkDetectionApi.lib.reports.ReportUtils;
//
//
//public class ByFileTypeReportDAL {
//	private static final Logger log = LoggerFactory.getLogger(ReportUtils.class);
//	private static Object syncLockObj = new Object();
//	public static final String baseQueryString = 
//			"select new map "
//			+ "(t.taskId as taskId,"
//			+ "t.source as source,"
//			+ "fs.submissionId as submissionId,"
//			+ "fs.filename as fileName,"
//			+ "fs.success as success,"
//			+ "fa.fileAttributeId as fileAttributeId,"
//			+ "fa.fileType as fileType,"
//			+ "I.score as score,"
//			+ "I.verdict as verdict,"
//			+ "I.analysisType as analysisType,"
//			+ "I.sha256 as sha256) "
//			+ "from Batch b "
//			+ "inner join b.tasks t "
//			+ "inner join t.fileSubmissions fs "
//			+ "inner join fs.fileAttributes fa "
//			+ "inner join fa.siAnalyses I "
//			+ "where b= :batchId and fa.fileType is not null";
//	
//	public static final String fileTypesQuery =
//			"select distinct fa.FileType "
//			+ "from FileAttribute fa "
//			+ "where fa.FileType is not null";
//	
//	public ByFileTypeReportDAL() {}
//	
//	public static JSONObject getReport(Batch batch) throws Exception {
//		EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
//		synchronized (syncLockObj) {
//			Set<String> fileTypes = new HashSet<String>();
//			// want to return
//			Map<String,Long> total_files = new HashMap<String,Long>();
//			Map<String,Long> static_files = new HashMap<String,Long>();
//			Map<String,Long> dropped_files = new HashMap<String,Long>();
//			Map<String,Long> static_suspicious = new HashMap<String,Long>();
//			Map<String,Long> static_malicious = new HashMap<String,Long>();
//			Map<String,Long> dynamic_files = new HashMap<String,Long>();
//			Map<String,Long> dynamic_suspicious = new HashMap<String,Long>();
//			Map<String,Long> dynamic_malicious = new HashMap<String,Long>();
//			int numTotalFiles = 0;
//			int numTotalSuspicious = 0;
//			int numTotalMalicious = 0;
//			
//			try {
//				// get stream using base query
//				Query baseQuery = entityManager.createQuery(baseQueryString);
//				baseQuery.setParameter("batchId", batch);
//				@SuppressWarnings("unchecked")
//				List<Map<String,Object>> resultList = baseQuery.getResultList();
//				entityManager.close();
//				
//				// tally up totals
//				System.out.println("length "+resultList.size());
//				for (Map<String,Object> row : resultList)
//				{
//					ByFileTypeReportRow rowData = new ByFileTypeReportRow(row);
//					
//					String fileType = rowData.getFileType();
//					fileTypes.add(fileType);
//					ByFileTypeReportDAL.safeAdditionHelper(total_files, fileType);
//					if (rowData.getAnalysisType().equals("static"))
//					{
//						numTotalFiles += 1;
//						ByFileTypeReportDAL.safeAdditionHelper(static_files, fileType);
//						if(rowData.getVerdict().equals("suspicious"))
//						{
//							numTotalSuspicious += 1;
//							ByFileTypeReportDAL.safeAdditionHelper(static_suspicious, fileType);
//						}
//						else if(rowData.getVerdict().equals("malicious"))
//						{
//							ByFileTypeReportDAL.safeAdditionHelper(static_malicious, fileType);
//						}
//					}
//					else if(rowData.getAnalysisType().equals("dynamic"))
//					{
//						if(rowData.getVerdict().equals("malicious"))
//						{
//							numTotalMalicious += 1;
//							ByFileTypeReportDAL.safeAdditionHelper(dynamic_malicious, fileType);
//						}
//					}
//				}
//
//				// place into JSON objects
//				JSONObject report = new JSONObject();
//				JSONObject data = new JSONObject();
//				// completion
//				ProcedurallyGeneratedReport pgReport = new ProcedurallyGeneratedReport(batch);
//				data.put("Completion", ((int)pgReport.getBatchCompletion().get("percent"))+"%");
//				// totals
//				JSONObject totals = new JSONObject();
//				totals.put("files", numTotalFiles);
//				totals.put("suspicious", numTotalSuspicious);
//				totals.put("malicious", numTotalMalicious);
//				data.put("total", totals);
//				// by FileType
//				for (String fileType : fileTypes)
//				{
//					// make sure each FileType has an entry
//					static_files = ByFileTypeReportDAL.insertZeroIfNull(static_files, fileType);
//					static_suspicious = ByFileTypeReportDAL.insertZeroIfNull(static_suspicious, fileType);
//					static_malicious = ByFileTypeReportDAL.insertZeroIfNull(static_malicious, fileType);
//					dynamic_files = ByFileTypeReportDAL.insertZeroIfNull(dynamic_files, fileType);
//					dynamic_malicious = ByFileTypeReportDAL.insertZeroIfNull(dynamic_malicious, fileType);
//					
//					// create report for this FileType
//					JSONObject j = new JSONObject();
//					JSONObject jStatic = new JSONObject();
//					JSONObject jDynamic = new JSONObject();
//					jStatic.put("files", static_files.get(fileType));
//					jStatic.put("suspicious", static_suspicious.get(fileType));
//					jStatic.put("malicious", static_malicious.get(fileType));
//					jDynamic.put("files", dynamic_files.get(fileType));
//					jDynamic.put("malicious", dynamic_malicious.get(fileType));
//					j.put("static", jStatic);
//					j.put("dynamic", jDynamic);
//					data.put(fileType, j);
//				}
//				report.put("data", data);
//				return report;
//				
//			} catch (Exception ex) {
//				if (entityManager != null && entityManager.getTransaction().isActive()) {
//					entityManager.getTransaction().rollback();
//				}
//				ex.printStackTrace();
//				return new JSONObject("failed");
//			} finally {
//				if (entityManager != null) {
//					entityManager.close();
//				}
//			}
//		}
//	}
//	
//	private static Map<String,Long> safeAdditionHelper(Map<String,Long> m, String fileType)
//	{
//		if(m.containsKey(fileType))
//		{
//			m.put(fileType, m.get(fileType)+1);
//		}
//		else
//		{
//			m.put(fileType, (long) 1);
//		}
//		return m;
//	}
//	
//	private static Map<String,Long> insertZeroIfNull(Map<String,Long> m, String fileType)
//	{
//		if(!m.containsKey(fileType)) {
//			m.put(fileType, (long) 0);
//		}
//		return m;
//	}
//}
