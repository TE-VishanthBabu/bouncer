package com.zorsecyber.bouncer.api.dao;

import org.json.JSONObject;

public class StaticDynamicReport {
	public int numFilesStatic;
	public int numSuspiciousStatic;
	public int numMaliciousStatic;
	public int numFileDynamic;
	public int numMaliciousDynamic;
	
	public StaticDynamicReport()
	{
	}
	
	public StaticDynamicReport(int errNumFiles)
	{
		this.numFilesStatic = errNumFiles;
	}
	
	public JSONObject toJson()
	{
		JSONObject staticReport = new JSONObject();
		staticReport.put("numFiles", numFilesStatic);
		staticReport.put("numSuspicious", numSuspiciousStatic);
		staticReport.put("numMalicious", numMaliciousStatic);
		JSONObject dynamicReport = new JSONObject();
		dynamicReport.put("numFiles", numFileDynamic);
		dynamicReport.put("numMalicious", numMaliciousDynamic);
		JSONObject report = new JSONObject();
		report.put("static", staticReport);
		report.put("dynamic", dynamicReport);
		return report;
	}

	public int getNumSuspiciousStatic() {
		return numSuspiciousStatic;
	}

	public void setNumSuspiciousStatic(int numSuspiciousStatic) {
		this.numSuspiciousStatic = numSuspiciousStatic;
	}

	public int getNumMaliciousStatic() {
		return numMaliciousStatic;
	}

	public void setNumMaliciousStatic(int numMaliciousStatic) {
		this.numMaliciousStatic = numMaliciousStatic;
	}

	public int getNumFileDynamic() {
		return numFileDynamic;
	}

	public void setNumFileDynamic(int numFileDynamic) {
		this.numFileDynamic = numFileDynamic;
	}

	public int getNumMaliciousDynamic() {
		return numMaliciousDynamic;
	}

	public void setNumMaliciousDynamic(int numMaliciousDynamic) {
		this.numMaliciousDynamic = numMaliciousDynamic;
	}
	
}
