package com.zorsecyber.bouncer.api.lib.reports;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.json.JSONArray;
import org.json.JSONObject;

import com.microsoft.applicationinsights.core.dependencies.apachecommons.lang3.StringUtils;
import com.zorsecyber.bouncer.api.dal.EntityManagerFactorySingleton;
import com.zorsecyber.bouncer.api.dal.TaskDAL;
import com.zorsecyber.bouncer.api.dao.Batch;
import com.zorsecyber.bouncer.api.dao.FileAttribute;
import com.zorsecyber.bouncer.api.dao.FileSubmission;
import com.zorsecyber.bouncer.api.dao.SIAnalysis;
import com.zorsecyber.bouncer.api.dao.SIIndicator;
import com.zorsecyber.bouncer.api.dao.Task;
import com.zorsecyber.bouncer.api.dao.User;
import com.zorsecyber.bouncer.api.dao.oauth2.AccessToken;
import com.zorsecyber.bouncer.api.lib.msgraph.MSGraph5;
import com.zorsecyber.bouncer.api.lib.status.TaskStatus;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Transactional
public class PGReportV1 extends ProcedurallyGeneratedReport {
	private static final Object syncLockObj = new Object();

	public PGReportV1(Batch batch) throws Exception {
		super(batch);
	}

	@Transactional
	public static JSONObject buildAvailableFilesView(JSONArray availableFiles) {
		EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
		JSONObject table = new JSONObject();
		synchronized (syncLockObj) {
			try {
				JSONArray headers = new JSONArray();
				headers.put("Filename");
				table.put("headers", headers);

				JSONArray rows = new JSONArray();
				for (Object o : availableFiles) {
					String filename = (String) o;
					if(!filename.contains("/")) {
						JSONObject row = new JSONObject().put("Filename", filename);
						rows.put(row);
					}
				}

				table.put("rows", rows);
			} catch (Exception ex) {
//				ex.printStackTrace();
				return null;
			} finally {
				if (entityManager != null) {
					// close EM
					entityManager.close();

				}
			}
			return new JSONObject().put("data", table);
		}

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Transactional
	public JSONObject buildBatchView(User user, int limit, int offset, String orderColumn, int orderDirection) {
		EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
		JSONObject table = new JSONObject();
			try {
				JSONArray headers = new JSONArray();
				headers.put(buildBasicHeader("batchId"));
				headers.put(buildBasicHeader("Batch"));
				headers.put(buildBasicHeader("Completion"));
				headers.put(buildBasicHeader("Clean"));
				headers.put(buildBasicHeader("Suspicious"));
				headers.put(buildBasicHeader("Malicious"));
				headers.put(buildBasicHeader("Actions"));
				headers.put(buildBasicHeader("Timestamp"));
				table.put("headers", headers);
				if (limit == 0) {
					long recordsTotal = getTableSize((Class) Batch.class, "user", user);
					table.put("recordsTotal", recordsTotal);
					table.put("recordsFiltered", recordsTotal);
					return new JSONObject().put("data", table);
				}
				List<Batch> batches = getBatchPage(entityManager, user, limit, offset, orderColumn, orderDirection);
				JSONArray rows = new JSONArray();
//				System.out.println("batches : " + batches.size());
				String batchName;
				for (Batch batch : batches) {
//					System.out.println(batch.getBatchId());
					JSONObject row = new JSONObject()
							.put("Actions", StringUtils.EMPTY);
					row.put("batchId", batch.getBatchId());
					batchName = batch.getName();
					if(batchName == null) {
						batchName = Long.toString(batch.getBatchId());
					}
					row.put("Batch", batchName);
					setBatch(batch);
					Map<String, Integer> batchCompletion = getBatchCompletion();
					if (batchCompletion.get("total") == -1) {
						row.put("Completion", "Processing...");
					} else {
						row.put("Completion", batchCompletion.get("percent"));
					}

					Map<String, Integer> CSMTotals = getBatchCSMTotals(batch);
					int clean = 0;
					int suspicious = 0;
					int malicious = 0;
					int total = 0;
					try {
						clean = CSMTotals.get("clean");
						suspicious = CSMTotals.get("suspicious");
						malicious = CSMTotals.get("malicious");
						total = CSMTotals.get("total");
					} catch (NullPointerException ex) {
						// ex.printStackTrace();
					}
					// avoid div by 0
					if(total == 0) {
						total = 1;
					}

					row.put("Clean", formatNumAndPercentString(clean, total));
					row.put("Suspicious", formatNumAndPercentString(suspicious, total));
					row.put("Malicious", formatNumAndPercentString(malicious, total));
					row.put("Timestamp", batch.getCreationTimestamp().toString());
					rows.put(row);
				}
				table.put("rows", rows);
			} catch (Exception ex) {
//				ex.printStackTrace();
				return null;
			} finally {
				if (entityManager != null) {
					// close EM
					entityManager.close();

				}
			}
			return new JSONObject().put("data", table);
		}
	
	@Transactional
	public JSONObject buildFiletypeSummaryView() {
		EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
		JSONObject table = new JSONObject();
			try {
				Boolean includeSanitized = sanitizedEnabledInBatch();
				JSONArray headers = new JSONArray();
				headers.put("File Type");
				headers.put("Total");
				headers.put("Suspicious");
				headers.put("Malicious");
				if(includeSanitized) {
					headers.put("Sanitized Suspicious");
					headers.put("Sanitized Malicious");
				}
				table.put("headers", headers);

				List<String> fileTypes = listScannedFileTypes(batch);
//				System.out.println("include sanitized?: "+includeSanitized);
				JSONArray rows = new JSONArray();
				long totalFiles = 0;
				long sanitizedTotalFiles = 0;
				for(String fileType : fileTypes) {
					JSONObject row = new JSONObject();
					row.put("File Type", fileType);
					Map<String, Long> fileTypeTotals = getFileTypeCSMTotals(fileType);
					totalFiles = fileTypeTotals.get("c")+fileTypeTotals.get("s")+fileTypeTotals.get("m");
					row.put("Total", totalFiles);
					row.put("Suspicious", formatNumAndPercentString(fileTypeTotals.get("s"), totalFiles));
					row.put("Malicious", formatNumAndPercentString(fileTypeTotals.get("m"), totalFiles));
					if(includeSanitized) {
						sanitizedTotalFiles = fileTypeTotals.get("cs")+fileTypeTotals.get("ss")+fileTypeTotals.get("sm");
						row.put("Sanitized Suspicious", formatNumAndPercentString(fileTypeTotals.get("ss"), sanitizedTotalFiles));
						row.put("Sanitized Malicious", formatNumAndPercentString(fileTypeTotals.get("sm"), sanitizedTotalFiles));
					}
					rows.put(row);
				}
				
				
				table.put("rows", rows);
			} catch (Exception ex) {
				ex.printStackTrace();
				return null;
			} finally {
				if (entityManager != null) {
					// close EM
					entityManager.close();

				}
			}
			return new JSONObject().put("data", table);
		}

	@Transactional
	public JSONObject buildTaskView(int limit, int offset, String orderColumn, int orderDirection) throws Exception {
		EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
		JSONObject table = new JSONObject();
		try {
			JSONArray headers = new JSONArray();
			headers.put(buildBasicHeader("Task"));
			headers.put(buildBasicHeader("Mailbox"));
			headers.put(buildBasicHeader("Status"));
//			headers.put(buildBasicHeader("Total"));
			headers.put(buildBasicHeader("Clean"));
			headers.put(buildBasicHeader("Suspicious"));
			headers.put(buildBasicHeader("Malicious"));
			table.put("headers", headers);
			if (limit == 0) {
				long recordsTotal = getTableSize((Class) Task.class, "batch", batch);
				table.put("recordsTotal", recordsTotal);
				table.put("recordsFiltered", recordsTotal);
				return new JSONObject().put("data", table);
			}
			List<Task> tasks = getTasksPage(entityManager, limit, offset, orderColumn, orderDirection);
			JSONArray rows = new JSONArray();
			TaskStatus status;
			for (Task task : tasks) {
				JSONObject row = new JSONObject();
				status = task.getStatus();
				log.debug("task "+task.getTaskId()+" status "+status.toString());
				int filesCompleted = task.getFilesCompleted();
				if (task.isComplete()) {
					status = TaskStatus.COMPLETE;
					// update in db
					TaskDAL.setTaskStatus(task.getTaskId(), TaskStatus.COMPLETE);
				}
				row.put("Status", status.toString());
				row.put("Task", task.getTaskId());
				row.put("Mailbox", task.getSource());;
				
				String clean = formatNumAndPercentString(task.getClean(), filesCompleted);
				String suspicious = formatNumAndPercentString(task.getSuspicious(), filesCompleted);
				String malicious = formatNumAndPercentString(task.getMalicious(), filesCompleted);

//				row.put("Total", numberOfFiles);
				row.put("Clean", clean);
				row.put("Suspicious", suspicious);
				row.put("Malicious", malicious);
				rows.put(row);
			}
			table.put("rows", rows);
		} catch (Exception ex) {
//			ex.printStackTrace();
			throw new Exception("Failed to generate taskView: " + ex.getMessage());
		} finally {
			if (entityManager != null) {
				// close EM
				entityManager.close();

			}
		}
		return new JSONObject().put("data", table);
	}

	public JSONObject buildFileDetailsView(FileAttribute fa) {
		JSONObject tables = new JSONObject();
		EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
			try {
				JSONObject detailsTable = new JSONObject();
				// create table headers
				JSONArray analysisHeaders = new JSONArray();
				analysisHeaders.put("Filename");
				analysisHeaders.put("sha256");
				detailsTable.put("headers", analysisHeaders);

				// begin tx
				// initialize rows array
				JSONObject analysisRows = new JSONObject();
				analysisRows.put("sha256", fa.getSha256());
				FileSubmission fs = fa.getSubmission();
				analysisRows.put("Filename", fs.getFilename());
				detailsTable.put("rows", analysisRows);
				
				// put s and d analysis verdicts
				String verdictS = "N/A";
				String verdictD = "N/A";
				String siAnalysisIdS = null;
				String siAnalysisIdD = null;
				String sanVerdictS = "N/A";
				String sanVerdictD = "N/A";
				String sanSiAnalysisIdS = null;
				String sanSiAnalysisIdD = null;
				List<SIAnalysis> siAnalyses = getSiAnalyses(entityManager, fa);
				for (SIAnalysis a : siAnalyses) {
						if (a.getAnalysisType().equals("static")) {
							if(a.getSanitized() == false) {
								verdictS = a.getVerdict();
								siAnalysisIdS = Long.toString(a.getIntellixId());
							} else {
								sanVerdictS = a.getVerdict();
								sanSiAnalysisIdS = Long.toString(a.getIntellixId());
							}
						} else {
							if(a.getSanitized() == false) {
								verdictD = a.getVerdict();
								siAnalysisIdD = Long.toString(a.getIntellixId());
							} else {
								sanVerdictD = a.getVerdict();
								sanSiAnalysisIdD = Long.toString(a.getIntellixId());
							}
						}
				}
				JSONObject analysisTable = new JSONObject();
				analysisRows = new JSONObject();
				analysisHeaders = new JSONArray();
				JSONObject analysisMetadata = new JSONObject();
				
				JSONObject sanitizeAnalysisTable = null;
				JSONObject sanitizeRows = new JSONObject();
				JSONArray sanitizeHeaders = new JSONArray();
				JSONObject sanitizeMetadata = new JSONObject();
				
				analysisHeaders.put("Static Verdict");
				analysisRows.put("Static Verdict", verdictS);
				analysisTable.put("headers", analysisHeaders);
				analysisTable.put("rows", analysisRows);
				// create metadata
				analysisMetadata.put("fileAttributeId", Long.toString(fa.getFileAttributeId()));
				analysisMetadata.put("siAnalysisIdS", siAnalysisIdS);
				analysisMetadata.put("siAnalysisIdD", siAnalysisIdD);
				analysisMetadata.put("verdict", capitalizeFirstLetter(fs.getVerdict()));
				analysisTable.put("metadata", analysisMetadata);
				
				if(siAnalysisIdD != null) {
					analysisRows.put("Dynamic Verdict", verdictD);
					analysisHeaders.put("Dynamic Verdict");
					if(fs.getSanitizeStatus() != null) {
						sanitizeAnalysisTable = new JSONObject();
						sanitizeHeaders.put("Sanitization Results");
						sanitizeRows.put("Sanitization Results", fs.getSanitizeStatus());
						if(sanSiAnalysisIdS != null) {
							sanitizeHeaders.put("Static Verdict");
							sanitizeRows.put("Static Verdict", sanVerdictS);
							if(sanSiAnalysisIdD != null) {
								sanitizeHeaders.put("Dynamic Verdict");
								sanitizeRows.put("Dynamic Verdict", sanVerdictD);
							}
						}
					}
				}
				
				if(sanitizeAnalysisTable != null) {
					sanitizeMetadata.put("fileAttributeId", Long.toString(fa.getFileAttributeId()));
					sanitizeMetadata.put("siAnalysisIdS", sanSiAnalysisIdS);
					sanitizeMetadata.put("siAnalysisIdD", sanSiAnalysisIdD);
					sanitizeMetadata.put("verdict", capitalizeFirstLetter(fs.getSanitizedVerdict()));
					
					sanitizeAnalysisTable.put("metadata", sanitizeMetadata);
					sanitizeAnalysisTable.put("headers", sanitizeHeaders);
					sanitizeAnalysisTable.put("rows", sanitizeRows);
				}
			
				tables.put("details", detailsTable)
				.put("analysis", analysisTable)
				.put("sanitizeAnalysis", sanitizeAnalysisTable);

			} catch (Exception ex) {
				ex.printStackTrace();
			} finally {
				if (entityManager != null) {
					// close EM
					entityManager.close();
				}
			}
			return new JSONObject().put("data", tables);
		}

	/**
	 * Build a jsonobject table with headers Filename, sha256, verdict and rows of
	 * data for each file. Refer to function above about how to format the return
	 * type (headers and rows)
	 * 
	 * @param Task task
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public JSONObject buildFileView(Task task, int limit, int offset, String orderColumn, int orderDirection, String q,
			String qColumn) {
		JSONObject table = new JSONObject();
		EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
			try {
				// create table headers
				JSONArray headers = new JSONArray();
				headers.put("Filename");
				headers.put("fileAttributeId");
				headers.put("sha256");
				headers.put("Verdict");
				table.put("headers", headers);
				if (limit == 0) {
					long recordsTotal = getTableSize((Class) FileSubmission.class, "task", task);
					table.put("recordsTotal", recordsTotal);
					table.put("recordsFiltered", recordsTotal);
					return new JSONObject().put("data", table);
				}
				// get filenameSubmissions
//				System.out.println("getting submissions limit " + limit + " offset " + offset);
				List<FileSubmission> fileSubmissions = getFilesubmissionsPage(entityManager, task, limit, offset, q, qColumn,
						orderColumn, orderDirection);
				// initialize rows array
				JSONArray rows = new JSONArray();
				for (FileSubmission fs : fileSubmissions) {
					JSONObject row = new JSONObject();
					row.put("Filename", fs.getFilename());
					row.put("fileAttributeId", getFileAttribute(fs).getFileAttributeId());
					row.put("sha256", fs.getSha256());
					// get the siAnalyses associated with the fs
					String verdict = fs.getVerdict();
					row.put("Verdict", verdict);
					rows.put(row);
				}
				table.put("rows", rows);
			} catch (Exception ex) {
//				ex.printStackTrace();
			} finally {
				if (entityManager != null) {
					// close EM
					entityManager.close();
				}
			}
			return new JSONObject().put("data", table);
		}
	
	public static JSONObject buildMailboxesView(AccessToken accessToken, int limit, int offset) {
		JSONObject table = new JSONObject();
		try  {
			JSONArray headers = new JSONArray()
					.put("checkbox")
					.put("Mailbox");
			table.put("headers", headers);
			if (limit == 0) {
//				table.put("recordsTotal", recordsTotal);
//				table.put("recordsFiltered", recordsTotal);
				return new JSONObject().put("data", table);
			}
			
			JSONArray rows = new JSONArray(); // initialize rows array
			try {
				
				MSGraph5 graph = new MSGraph5(accessToken.getAccessToken());
				List<String> mailboxes = graph.getMailboxes(offset, limit);
				for(String mailbox : mailboxes){
					JSONObject row = new JSONObject();
					row.put("checkbox", "");
					row.put("Mailbox", mailbox);
					row.put("auth", graph.canGraphUserAccessMailbox(mailbox));
					rows.put(row);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				log.warn("Could not get mailboxes from the graph client");
				throw new Exception(ex);
			}
			table.put("rows", rows);
			return new JSONObject().put("data", table);
		} catch (Exception ex) {
			ex.printStackTrace();
			return new JSONObject().put("data", table);
		}
	}

	/**
	 * SiAnalysis - analysis of a file SIIndicator - Indicator of static analysis
	 * results siAnalysis -> SIIndicator is one-many goal: fetch a table for one
	 * SiAnalysis containing its static indicators
	 * 
	 * O(table) -> { A('headers': [headers]), A(rows -> { A([v, v]) }) }
	 * 
	 */

	public JSONObject buildIndicatorView(SIAnalysis siAnalysis) {
//		System.out.println("starting indicator view");
		JSONObject table = new JSONObject();
		EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
			try {
				// create table headers
				JSONArray headers = new JSONArray();
				headers.put("Name");
				headers.put("Severity");
				headers.put("Description");
				table.put("headers", headers);

				List<SIIndicator> indicators = getSiIndicators(entityManager, siAnalysis);
				JSONArray rows = new JSONArray(); // initialize rows array
				for (SIIndicator indicator : indicators) {
					JSONObject row = new JSONObject();
					row.put("Severity", indicator.getSeverity());
					row.put("Name", indicator.getName());
					row.put("Description", indicator.getDescription());
					rows.put(row);
				}
				table.put("rows", rows);
			} catch (Exception ex) {
//				ex.printStackTrace();
				return null;
			} finally {
				if (entityManager != null) {
					entityManager.close(); // close tx
				}
			}
		return new JSONObject().put("data", table);
	}

	private JSONObject buildBasicHeader(String name) {
		return buildHeader(name, name);
	}

	private JSONObject buildHeader(String name, String data) {
		return new JSONObject().put("name", name).put("data", data);
	}

}
