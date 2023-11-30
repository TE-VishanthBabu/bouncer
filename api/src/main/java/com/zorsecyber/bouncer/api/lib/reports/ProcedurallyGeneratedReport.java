package com.zorsecyber.bouncer.api.lib.reports;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaBuilder.SimpleCase;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zorsecyber.bouncer.api.dal.EntityManagerFactorySingleton;
import com.zorsecyber.bouncer.api.dal.TaskDAL;
import com.zorsecyber.bouncer.api.dao.Batch;
import com.zorsecyber.bouncer.api.dao.FileAttribute;
import com.zorsecyber.bouncer.api.dao.FileSubmission;
import com.zorsecyber.bouncer.api.dao.SIAnalysis;
import com.zorsecyber.bouncer.api.dao.SIIndicator;
import com.zorsecyber.bouncer.api.dao.Sanitize;
import com.zorsecyber.bouncer.api.dao.Task;
import com.zorsecyber.bouncer.api.dao.User;
import com.zorsecyber.bouncer.api.lib.status.TaskStatus;

@SuppressWarnings("unchecked")
public class ProcedurallyGeneratedReport {
	private static final String submissionsApiUrl = "https://hauberk-api-management.azure-api.net/hauberk-detection-api-dev/";
	private static final Logger log = LoggerFactory.getLogger(ProcedurallyGeneratedReport.class);
	protected Batch batch;
	private static final int maxPageSize = 10000;

	private static final String getFileTypeCSMTotalsQueryString = "select new map "
			+ "(sum(case when fs.verdict='suspicious' then 1 else 0 end) as s, "
			+ "sum(case when fs.verdict='malicious' then 1 else 0 end) as m, "
			+ "sum(case when fs.sanitizedVerdict='suspicious' then 1 else 0 end) as ss, "
			+ "sum(case when fs.sanitizedVerdict='malicious' then 1 else 0 end) as sm, "
			+ "sum(case when fs.verdict='clean' then 1 else 0 end) as c, "
			+ "sum(case when fs.sanitizedVerdict='clean' then 1 else 0 end) as cs) " + "from Task t "
			+ "inner join t.fileSubmissions fs inner join fs.fileAttributes fa "
			+ "where t.batch= :batchId and fa.fileType= :fileType";

	public ProcedurallyGeneratedReport(Batch batch) {
		this.setBatch(batch);
	}

	protected JSONArray getAvailableFiles(long userId, String secret, String dataSource)
			throws ClientProtocolException, IOException, URISyntaxException {
		String url = ProcedurallyGeneratedReport.submissionsApiUrl + dataSource + "/files";

		URIBuilder params = new URIBuilder(url);
		params.setParameter("clientId", Long.toString(userId));
		params.setParameter("secret", secret);
		params.setParameter("dir", "");
		CloseableHttpClient httpclient = HttpClients.createDefault();
//		System.out.print(params.build().toString());
		HttpGet httpGet = new HttpGet(params.build());

		CloseableHttpResponse response = httpclient.execute(httpGet);
		HttpEntity responseEntity = response.getEntity();
		String responseBody = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);
//		System.out.println("Retrieved report : " + responseBody);

		JSONObject jsonResponse = new JSONObject(responseBody);
		return jsonResponse.getJSONArray("data");
	}

	protected String capitalizeFirstLetter(String s) {
		return s.substring(0, 1).toUpperCase() + s.substring(1);
	}

	protected float percentage(long num, long total) {
		if (total == 0) {
			return 0;
		}
		return clamp(100 * (float) num / (float) total, 0, 100);
	}

	protected String formatNumAndPercentString(long num, long total) {
		if (num == 0) {
			return "0";
		} else if (num == total) {
			return num + " (100%)";
		}
		return num + " (" + round(percentage(num, total), 2) + "%)";
	}

	public static double round(float d, int decimals) {
		return Math.round(d * Math.pow(10, decimals)) / Math.pow(10, decimals);
	}

	protected List<String> listScannedFileTypes(Batch batch) throws Exception {
		List<String> fileTypes = new ArrayList<String>();
		EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
		try {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<String> criteriaQuery = cb.createQuery(String.class);
			Root<Task> root = criteriaQuery.from(Task.class);

			Join<Task, FileSubmission> join = root.join("fileSubmissions", JoinType.INNER);
			List<Predicate> criteria = new ArrayList<Predicate>();
			criteria.add(cb.and(cb.equal(root.get("batch"), batch), cb.notEqual(join.get("verdict"), "failed")));
			join = join.join("fileAttributes", JoinType.INNER);
			criteriaQuery.select(join.get("fileType")).distinct(true);
			criteriaQuery.where(cb.and(criteria.toArray(new Predicate[] {})));

			fileTypes = entityManager.createQuery(criteriaQuery).getResultList();
			return fileTypes;
		} catch (Exception ex) {
			throw new Exception("Unable to retrieve list of fileTypes: " + ex.getMessage());
		} finally {
			if (entityManager != null) {
				entityManager.close();
			}
		}
	}

	protected Map<String, Long> getFileTypeCSMTotals(String fileType) throws Exception {
		EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
		try {
			Query q = entityManager.createQuery(getFileTypeCSMTotalsQueryString);
			q.setParameter("batchId", batch);
			q.setParameter("fileType", fileType);
			Map<String, Long> totals = (Map<String, Long>) q.getSingleResult();
			// adjust sanitized clean total to include original clean files
			totals.put("cs", totals.get("cs") + totals.get("c"));
			return totals;
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new Exception("Unable to retrieve fileType CSM totals: " + ex.getMessage());
		} finally {
			if (entityManager != null) {
				entityManager.close();
			}
		}
	}

	protected Boolean sanitizedEnabledInBatch() throws Exception {
		EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
		try {
			Query q = entityManager.createQuery("select sum(case when t.sanitize=true then 1 else 0 end) "
					+ "from Batch b inner join b.tasks t where b= :batchId");
			q.setParameter("batchId", batch);
			long enabledCount = (long) q.getSingleResult();
			return enabledCount > 0;
		} catch (Exception ex) {
			throw new Exception("Unable to retrieve sanitize enabled status: " + ex.getMessage());
		} finally {
			if (entityManager != null) {
				entityManager.close();
			}
		}
	}

	protected Map<String, Integer> getBatchCSMTotals(Batch batch) {
		EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
		try {
			Map<String, Integer> totals = new HashMap<String, Integer>();
			int clean = 0;
			int suspicious = 0;
			int malicious = 0;
			List<Task> tasks = (List<Task>) batch.getTasks();
			for (Task t : tasks) {
				clean += t.getClean();
				suspicious += t.getSuspicious();
				malicious += t.getMalicious();
			}
			totals.put("clean", clean);
			totals.put("suspicious", suspicious);
			totals.put("malicious", malicious);
			totals.put("total", clean + suspicious + malicious);
			return totals;
		} catch (Exception ex) {
//			ex.printStackTrace();
			return null;
		} finally {
			if (entityManager != null) {
				entityManager.close();
			}
		}
	}

	/**
	 * Calculates the completion status of a batch
	 * @return JSONObject containing total number of tasks in batch and number of
	 * completed tasks in batch
	 */
	protected Map<String, Integer> getBatchCompletion() {
		EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
		try {
			if (!entityManager.contains(batch)) {
				// attach batch to session
				this.setBatch(entityManager.find(Batch.class, batch.getBatchId()));
			}
			Set<Task> tasks = batch.getUniqueTasks();
			int completed = 0;
			int total = tasks.size();
			Map<String, Integer> totals = new HashMap<String, Integer>();
			TaskStatus status;
			for (Task task : tasks) {
				// set task complete if it is complete
				status = task.getStatus();
				if(status != TaskStatus.COMPLETE && task.isComplete()) {
					status = TaskStatus.COMPLETE;
					TaskDAL.setTaskStatus(task.getTaskId(), TaskStatus.COMPLETE);
				}
				if(status == TaskStatus.COMPLETE || status == TaskStatus.FAILED) {
					completed++;
				}
			}
			totals.put("total", total);
			totals.put("percent", (int) percentage(completed, total));
			return totals;
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		} finally {
			if (entityManager != null) {
				// close EM
				entityManager.close();

			}
		}
	}
	

	protected static float clamp(float val, float min, float max) {
		return Math.max(min, Math.min(max, val));
	}

	protected <T> List<T> getPage(EntityManager entityManager, Class<T> type, String critField, T critObj, int limit,
			int offset, String orderColumn, int orderDirection, String q, String qColumn, List<String> selectCases)
			throws Exception {
		try {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<T> criteriaQuery = cb.createQuery(type);
			Root<T> root = criteriaQuery.from(type);
			List<Predicate> criteria = new ArrayList<Predicate>();
			criteria.add(cb.equal(root.get(critField), critObj));
			if (q != null) {
				criteria.add(cb.like(root.<String>get(qColumn), q + "%"));
			}
			Path<String> orderCol = root.get(orderColumn);
			SimpleCase<String, Integer> orderCase = cb.selectCase(orderCol);
			int i = 0;
			if (selectCases != null) {
				for (i = 1; i < selectCases.size(); i++)
					orderCase = orderCase.when(selectCases.get(i), i);
			}
			criteriaQuery.where(cb.and(criteria.toArray(new Predicate[] {})));
			Order order;
			if (orderDirection == 1) {
				if (selectCases != null)
					order = cb.asc(orderCase.otherwise(0));
				else
					order = cb.asc(orderCol);
				criteriaQuery.orderBy(order);
			} else {
				if (selectCases != null)
					order = cb.desc(orderCase.otherwise(0));
				else
					order = cb.desc(orderCol);
				criteriaQuery.orderBy(order);
			}
			List<T> result = entityManager.createQuery(criteriaQuery).setMaxResults(limit).setFirstResult(offset)
					.getResultList();
			return result;
		} catch (Exception ex) {
			throw new Exception("Could not retrieve page: " + ex.getMessage());
		}
	}

	protected <T> Long getTableSize(Class<T> type, String critField, T critObj) {
		EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
		try {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<Long> cr = cb.createQuery(Long.class);
			Root<T> root = cr.from(type);
			cr.select(cb.count(root)).where(cb.equal(root.get(critField), critObj));
			return entityManager.createQuery(cr).getSingleResult();
		} catch (Exception ex) {
//			ex.printStackTrace();
			return null;
		} finally {
			if (entityManager != null) {
				entityManager.close();
			}
		}
	}

	protected List<Batch> getBatchPage(EntityManager entityManager, User user, int limit, int offset,
			String orderColumn, int orderDirection) throws Exception {
		if (limit == -1) {
			limit = maxPageSize;
		}
		return getPage(entityManager, (Class) Batch.class, "user", user, limit, offset, orderColumn, orderDirection,
				null, null, null);
	}

	protected List<FileSubmission> getFilesubmissionsPage(EntityManager entityManager, Task task, int limit, int offset,
			String q, String qColumn, String orderColumn, int orderDirection) throws Exception {
		if (limit == -1) {
			limit = maxPageSize;
		}
		List<String> orderCases = null;
		if (orderColumn.equals("verdict")) {
			orderCases = new ArrayList<String>();
			orderCases.add("clean");
			orderCases.add("suspicious");
			orderCases.add("malicious");
		}
		return getPage(entityManager, (Class) FileSubmission.class, "task", task, limit, offset, orderColumn,
				orderDirection, q, qColumn, orderCases);
	}

	protected List<Task> getTasksPage(EntityManager entityManager, int limit, int offset, String orderColumn,
			int orderDirection) throws Exception {
		if (limit == -1) {
			limit = maxPageSize;
		}
		return getPage(entityManager, (Class) Task.class, "batch", batch, limit, offset, orderColumn, orderDirection,
				null, null, null);
	}

	protected List<SIAnalysis> getSiAnalyses(EntityManager entityManager, FileAttribute fa) throws Exception {
		return getPage(entityManager, (Class) SIAnalysis.class, "fileAttribute", fa, maxPageSize, 0, "fileAttribute",
				-1, null, null, null);
	}

	protected List<Sanitize> getSanitizes(EntityManager entityManager, FileAttribute fa) throws Exception {
		return getPage(entityManager, (Class) Sanitize.class, "fileAttribute", fa, maxPageSize, 0, "fileAttribute", -1,
				null, null, null);
	}

	protected List<SIIndicator> getSiIndicators(EntityManager entityManager, SIAnalysis siA) throws Exception {
		return getPage(entityManager, (Class) SIIndicator.class, "siAnalysis", siA, maxPageSize, 0, "siAnalysis", -1,
				null, null, null);
	}

	protected FileAttribute getFileAttribute(FileSubmission fs) {
		return ((List<FileAttribute>) fs.getFileAttributes()).get(0);
	}

	public void setBatch(Batch batch) {
		this.batch = batch;
	}

}
