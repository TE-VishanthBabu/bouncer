package com.zorsecyber.bouncer.core.dependencies;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zorsecyber.bouncer.core.dao.SIIndicator;

/**
 * This class represents a Sophos Intelix analysis report. 
 */
public class DeepSecureReport extends ApiAnalysisReport {
	protected static final Logger log = LoggerFactory.getLogger(DeepSecureReport.class);
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	
	public static String reportsSubdir = "reports";
	
	public int staticIndicatorCount = 0;
	public int maliciousActivityCount = 0;
	public String fileName;
	
	/**
	 * The constructor parses a Sophos Intelix JSON report and instantiates a
	 * SophosIntelixReport object
	 * @param jsonData	The json data used to construct the report object
	 */
	public DeepSecureReport(JSONObject raw, String fileName) {
		super(raw);
		this.fileName = fileName;
		sha256 = fileName;
}
	
	
//	public static DeepSecureReport getReportFromFile(String sha256, String dataDir, String analysisType) throws Exception
//	{
//		return new DeepSecureReport(ApiAnalysisReport.getReportFromFile(sha256, dataDir, analysisType).json);
//	}
}
