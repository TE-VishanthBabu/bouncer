package com.zorsecyber.bouncer.core.dal;

import java.io.FileReader;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azure.storage.file.share.ShareFileClient;
import com.controlcyberrisk.siaft.dependencies.HttpUtils;
import com.zorsecyber.bouncer.core.dao.FileShare;
import com.zorsecyber.bouncer.core.dao.ReportRepo;
import com.zorsecyber.bouncer.core.dependencies.SophosIntelixReport;

/**
 * This contains methods used to interact with the Report Repo
 */
public class ReportRepoDAL {
	private static final Logger log = LoggerFactory.getLogger(ReportRepoDAL.class);
	// 4MiB blocks
	public static final int DEFAULT_BUFFER_SIZE = 8192; 

	public ReportRepoDAL() {
	}

	  /**
	   * This method checks whether a specific analysis report exists in the repo.
	   * @param sha256 The sha256 checksum of the file.
	   * @param analysisType The type of report to look for.
	   * @return Boolean yes if report exists.
	   */
	public static boolean reportExists(String sha256, String analysisType)
	{
		String fileName = analysisType+"/"+sha256+".json";
		log.trace("Looking for "+analysisType+"/"+sha256+".json");
		try {
			ShareFileClient fileClient = FileShare.getDatalakeFileClient(fileName, ReportRepo.ReportsDataLakeFSName);
			return fileClient.exists();
		} catch (Exception e) {
			System.out.println("exception: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

	  /**
	   * This method saves the contents from a report in the repo to a JSONObject.
	   * @param sha256 The sha256 checksum of the file.
	   * @return JSONObject The report file contents.
	   */
	public static JSONObject getReportJson(String sha256, String dataDir, String analysisType) throws Exception
	{
		String jsonFilePath = dataDir + HttpUtils.sanitize_path(SophosIntelixReport.reportsSubdir) + analysisType + "/"
				+ sha256 + ".json";
		log.info("getting report : "+jsonFilePath);
		JSONObject reportjson = new JSONObject();
//		 parse the report file as JSON
			JSONTokener tokener = new JSONTokener(new FileReader(jsonFilePath));
			reportjson = new JSONObject(tokener);
		
		/** generate an SIreport object from JSON **/
		return reportjson;
	}
}
