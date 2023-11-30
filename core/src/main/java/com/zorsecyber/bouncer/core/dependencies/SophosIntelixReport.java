package com.zorsecyber.bouncer.core.dependencies;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zorsecyber.bouncer.core.dao.SIIndicator;
import com.zorsecyber.bouncer.core.exceptions.ApiAnalysisReportException;

/**
 * This class represents a Sophos Intelix analysis report.
 */
public class SophosIntelixReport extends ApiAnalysisReport {
	protected static final Logger log = LoggerFactory.getLogger(SophosIntelixReport.class);
	private SimpleDateFormat siTimestampFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	private SimpleDateFormat siDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public String status = new String();
	public String analysisType = new String();
	public String mimeType = new String();
	public String jobUuid = new String();
	public Date timestamp = new Date();
	public JSONObject reputation = new JSONObject();
	public int reportLevel = 0;
	public int score = 0;

	public static String reportsSubdir = "reports";

	public int staticIndicatorCount = 0;
	public int maliciousActivityCount = 0;


	/**
	 * The constructor parses a Sophos Intelix JSON report and instantiates a
	 * SophosIntelixReport object
	 * 
	 * @param jsonData The json data used to construct the report object
	 */
	public SophosIntelixReport(JSONObject raw) {
		super(raw);
		if(json == null) {
			throw new ApiAnalysisReportException("json data is null");
		}
		try {
			status = json.getString("status");
		} catch (Exception e) {
			throw new ApiAnalysisReportException(e);
		}
		try {
			reportLevel = json.getInt("report_level");
		} catch (Exception e) {
		}
		try {
			jobUuid = json.getString("job_uuid");
		} catch (Exception e) {
		}
		try {
			analysisType = json.getString("analysis_type");
		} catch (Exception e) {
		}
		try {
			timestamp = siTimestampFormat.parse(raw.getString("submission"));
		} catch (Exception e) {
		}
		try {
			score = json.getInt("score");
		} catch (Exception e) {
		}
		try {
			sha256 = json.getJSONObject("analysis_subject").getString("sha256");
		} catch (Exception e) {
		}
		try {
			mimeType = json.getJSONObject("analysis_subject").getString("mime_type");
		} catch (Exception e) {
		}
		try {
			reputation = json.getJSONObject("reputation");
		} catch (Exception e) {
		}
	}
	
	/**
	 * Create a basic json report for a failed file to pass to SophosIntelixDBWriter
	 * This is required when analysis fails and the retrieved report is null
	 * @param analysisType
	 * @return SophosIntelixReport
	 */
	public static SophosIntelixReport createFailedReportTemplate(String analysisType) {
		JSONObject failedReportJson = new JSONObject()
		.put("status", SophosIntelixAPI.STATUS_ERROR)
		.put("analysis_type", analysisType);
		return new SophosIntelixReport(failedReportJson);
	}

	public List<SIIndicator> getStaticIndicators() {
		List<SIIndicator> indicators = new ArrayList<SIIndicator>();
		JSONArray analysisSummary = new JSONArray();
		try {
			analysisSummary = json.getJSONArray("analysis_summary");
		} catch (Exception e) {
			log.error("Could not get analysis_summary field" + e.getMessage());
		}
		for (int i = 0; i < analysisSummary.length(); i++) {
			JSONObject indicatorJson = analysisSummary.getJSONObject(i);
			SIIndicator indicator = new SIIndicator();
			indicator.setSeverity(indicatorJson.getLong("severity"));
			indicator.setName(indicatorJson.getString("name"));
			indicator.setDescription(indicatorJson.getString("description"));
			indicators.add(indicator);
		}
		return indicators;
	}

	public void sumStaticIndicators() {
		int total = 0;
		JSONArray indicators = new JSONArray();
		try {
			indicators = json.getJSONArray("analysis_summary");
			total += indicators.length();
		} catch (Exception e) {
		}

		staticIndicatorCount = total;

	}

	public void sumMaliciousActivity() {
		int total = 0;
		JSONArray suspicious = new JSONArray();
		JSONArray malicious = new JSONArray();
		try {
			suspicious = json.getJSONObject("malicious_activity").getJSONArray("Suspicious");
			total += suspicious.length();
		} catch (Exception e) {
		}
		try {
			malicious = json.getJSONObject("malicious_activity").getJSONArray("Malicious");
			total += malicious.length();
		} catch (Exception e) {
		}

		maliciousActivityCount = total;

	}

	public Date dateFromString(String s) throws ParseException {
		return this.siDateFormat.parse(s);
	}

	public static SophosIntelixReport getReportFromFile(String sha256, String dataDir, String analysisType)
			throws Exception {
		return new SophosIntelixReport(ApiAnalysisReport.getReportFromFile(sha256, dataDir, analysisType).json);
	}
}
