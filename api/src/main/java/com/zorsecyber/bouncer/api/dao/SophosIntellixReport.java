package com.zorsecyber.bouncer.api.dao;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONObject;

public class SophosIntellixReport {
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public JSONObject json = new JSONObject();
	public String status = new String();
	public String analysisType = new String();
	public String sha256 = new String();
	public String mime_type = new String();
	public String job_uuid = new String();
	public Date timestamp = new Date();
	public int report_level = 0;
	public int score = 0;
	
	public static String reportsSubdir = "reports";
	
	public int staticIndicatorCount = 0;
	public int maliciousActivityCount = 0;
	
	public SophosIntellixReport(JSONObject raw) {
		this.json = raw;
		try {
		this.status = raw.getString("status");
		} catch(Exception e) {}
		try {
			report_level = raw.getInt("report_level");
		} catch (Exception e) {}
		try {
		this.job_uuid = raw.getString("job_uuid");
		} catch (Exception e) {}
		try {
		this.analysisType = raw.getString("analysis_type");
		} catch (Exception e) {}
		try {
			this.timestamp = this.sdf.parse(raw.getString("submission"));
		} catch (Exception e) {}
		try {
		this.score = raw.getInt("score");
	} catch (Exception e) {}
		try {
			this.sha256 = raw.getJSONObject("analysis_subject").getString("sha256");
		} catch (Exception e) {}
		try {
			this.mime_type = raw.getJSONObject("analysis_subject").getString("mime_type");
		} catch (Exception e) {}

}
	
	public void sumStaticIndicators() {
		int total = 0;
		JSONArray indicators = new JSONArray();
		try {
		indicators = this.json.getJSONArray("analysis_summary");
		total += indicators.length();
		} catch(Exception e) {}
		
		this.staticIndicatorCount = total;
		
	}
	
	public void sumMaliciousActivity() {
		int total = 0;
		JSONArray suspicious = new JSONArray();
		JSONArray malicious = new JSONArray();
		try {
		suspicious = this.json.getJSONObject("malicious_activity")
				.getJSONArray("Suspicious");
		total += suspicious.length();
		} catch(Exception e) {}
		try {
		malicious = this.json.getJSONObject("malicious_activity")
				.getJSONArray("Malicious");
		total += malicious.length();
		} catch(Exception e) {}
		
		this.maliciousActivityCount = total;
		
	}
//	create table siaft.Intellix (
//			PK_FileAttributeID int(16) not null primary key,
//			  FK_AnalysisID int(16),
//			  job_uuid varchar(256),
//			  score int(32),
//			  status varchar(16),
//			  report_level int(4),
//			  analysis_type varchar(16),
//			  mime_type varchar(64),
//			  sha256 varchar(256),
//			  staticIndicatorCount int(16),
//			  maliciousActivityCount int(16),
//			  submission_timestamp DATETIME,
//			  timestamp timestamp not null default current_timestamp
//			);
	
	public String[][] generateInsertArgs(long fileAttributeID, long analysisID) {
		this.sumMaliciousActivity();
		this.sumStaticIndicators();
		String[] fields = {"PK_AnalysisID", "FK_FileAttributeID","job_uuid", "score", "status", "report_level", "analysis_type",
		                   "mime_type", "sha256", "staticIndicatorCount", "maliciousActivityCount", "submission_timestamp"};
		String[] values = {Long.toString(analysisID), Long.toString(fileAttributeID), this.job_uuid, Integer.toString(this.score),
				this.status, Integer.toString(this.report_level), this.analysisType, this.mime_type, this.sha256, Integer.toString(this.staticIndicatorCount),
				Integer.toString(this.maliciousActivityCount), this.dateFormat.format(this.timestamp)};
		return new String[][] {fields, values};
	}
	
	
}
