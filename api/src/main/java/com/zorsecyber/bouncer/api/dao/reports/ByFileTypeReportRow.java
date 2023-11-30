package com.zorsecyber.bouncer.api.dao.reports;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ByFileTypeReportRow {
	
	public ByFileTypeReportRow(Map<String,Object> row)
	{
		this.taskId = (long) row.get("taskId");
		this.source = (String) row.get("source");
		this.submissionId = (long) row.get("submissionId");
		this.fileName = (String) row.get("fileName");
		this.success = (Boolean) row.get("success");
		this.fileAttributeId = (long) row.get("fileAttributeId");
		this.fileType = (String) row.get("fileType");
		this.score = (int) row.get("score");
		this.verdict = (String) row.get("verdict");
		this.analysisType = (String) row.get("analysisType");
		this.sha256 = (String) row.get("sha256");
	}

	private long taskId;
	private String source;
	private long submissionId;
	private String fileName;
	private Boolean success;
	private long fileAttributeId;
	private String fileType;
	private int score;
	private String verdict;
	private String analysisType;
	private String sha256;

	@Override
	public String toString() {
		return "ByFileTypeReport [taskId=" + taskId + ", source=" + source + ", submissionId=" + submissionId
				+ ", fileName=" + fileName + ", success=" + success + ", fileAttributeId=" + fileAttributeId
				+ ", fileType=" + fileType + ", score=" + score + ", verdict=" + verdict + ", analysisType="
				+ analysisType + ", sha256=" + sha256 + "]";
	}
	
}
