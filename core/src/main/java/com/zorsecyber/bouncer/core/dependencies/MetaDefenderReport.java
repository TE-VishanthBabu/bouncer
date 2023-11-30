package com.zorsecyber.bouncer.core.dependencies;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zorsecyber.bouncer.core.dal.MDAnalysisDAL;
import com.zorsecyber.bouncer.core.exceptions.ApiAnalysisReportException;
import com.zorsecyber.bouncer.core.exceptions.MetadefenderSanitzeBlockedException;

/**
 * This class represents a MetaDefender analysis report.
 */
public class MetaDefenderReport extends ApiAnalysisReport {
	private static final Logger log = LoggerFactory.getLogger(MDAnalysisDAL.class);
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public JSONObject json = new JSONObject();
	public String fileId = new String();
	public String dataId = new String();
	public String sha256 = new String();
	public String md5 = new String();
	String result = new String();
	private JSONObject processInfo = new JSONObject();
	private JSONObject fileInfo = new JSONObject();
//	private JSONObject scanDetails = new JSONObject();
	private JSONObject sanitized = new JSONObject();
	private String filePath = new String();
	public Date timestamp = new Date();
	public int scanTime = 0;
	public int progressPercentage = 0;
	public boolean completed = false;

	public static final String failureReportsSubdir = "out/Sanitization Failed/1003/";
	public static String reportsSubdir = "reports/metadefender/";

	public int staticIndicatorCount = 0;
	public int maliciousActivityCount = 0;

	/**
	 * The constructor parses a MetaDefender JSON report and instantiates a
	 * SophosIntelixReport object
	 * 
	 * @param jsonData The json data used to construct the report object
	 * @throws Exception
	 */
	public MetaDefenderReport(JSONObject jsonData) throws MetadefenderSanitzeBlockedException {
		super(jsonData);
		try {
			processInfo = jsonData.getJSONObject("process_info");
		} catch (Exception e) {
		}
		try {
			fileInfo = jsonData.getJSONObject("file_info");
		} catch (Exception e) {
		}
		try {
			fileId = jsonData.getString("file_id");
		} catch (Exception e) {
		}
		try {
			dataId = jsonData.getString("data_id");
		} catch (Exception e) {
		}
		try {
			this.timestamp = sdf.parse(fileInfo.getString("upload_timestamp"));
		} catch (Exception e) {
		}
		try {
			progressPercentage = processInfo.getInt("progress_percentage");
			result = processInfo.getString("result");
//			log.info("Result : " + result);
			if (result.equals("Blocked")) {
				throw new MetadefenderSanitzeBlockedException("Sanitization blocked");
			}
			if (progressPercentage == 100 && result.equals("Allowed")) {
				try {
					sanitized = jsonData.getJSONObject("sanitized");
					if (sanitized.getInt("progress_percentage") == 100) {
						completed = true;
						filePath = sanitized.getString("file_path");
						log.info("Retrieved file path : " + filePath);
					}
				} catch (Exception e) {
				}
			}
		} catch (JSONException e) {
		}
		try {
			sha256 = fileInfo.getString("sha256");
			md5 = fileInfo.getString("md5");
		} catch (Exception e) {
		}
//		try {
//			scanDetails = processInfo.getJSONObject("scan_details");
//		} catch (Exception e) {}

	}

	public void saveSanitizedFile(File sanitizedFile) throws IOException {
		log.info("Retrieving file from " + filePath);
		try (BufferedInputStream in = new BufferedInputStream(new URL(filePath).openStream());
				FileOutputStream fileOutputStream = new FileOutputStream(sanitizedFile)) {
			byte dataBuffer[] = new byte[1024];
			int bytesRead;
			while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
				fileOutputStream.write(dataBuffer, 0, bytesRead);
			}
			fileOutputStream.close();
		} catch (IOException e) {
			// handle exception
			e.printStackTrace();
		}
	}

	public static MetaDefenderReport getReportFromFile(String sha256, String dataDir, String analysisType)
			throws Exception {
		return new MetaDefenderReport(ApiAnalysisReport.getReportFromFile(sha256, dataDir, analysisType).json);
	}

	@Override
	public String toString() {
		return "MetaDefenderReport [sdf=" + sdf + ", dateFormat=" + dateFormat + ", json=" + json + ", fileId=" + fileId
				+ ", dataId=" + dataId + ", sha256=" + sha256 + ", md5=" + md5 + ", processInfo=" + processInfo
				+ ", fileInfo=" + fileInfo + ", sanitized=" + sanitized + ", filePath=" + filePath + ", timestamp="
				+ timestamp + ", scanTime=" + scanTime + ", progressPercentage=" + progressPercentage + ", completed="
				+ completed + ", staticIndicatorCount=" + staticIndicatorCount + ", maliciousActivityCount="
				+ maliciousActivityCount + "]";
	}
}
