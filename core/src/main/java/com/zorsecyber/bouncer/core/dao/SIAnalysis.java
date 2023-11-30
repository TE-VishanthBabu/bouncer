package com.zorsecyber.bouncer.core.dao;

import java.util.Collection;
import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

import com.nteligen.hq.dhs.siaft.dao.Analysis;
import com.nteligen.hq.dhs.siaft.dao.FileAttribute;

/**
 * This class represents a static or dynamic Analysis by Sophos Intelix.
 */
@Entity
@Table(name = "Intellix")
public class SIAnalysis
{

	  @Id
	  @GeneratedValue(strategy = GenerationType.AUTO)
	  @Column(name = "PK_IntellixID")
	  private long intellixId;
	  
	  @OneToMany(mappedBy = "siAnalysis",
			  	 fetch = FetchType.EAGER,
	             cascade = CascadeType.PERSIST,
	             orphanRemoval = true)
	  private Collection<SIIndicator> siIndicators;

	  @ManyToOne(cascade = CascadeType.ALL)
	  @JoinColumn(name = "FK_AnalysisID")
	  private Analysis analysis;

	  @ManyToOne(cascade = CascadeType.ALL)
	  @JoinColumn(name = "FK_FileAttributeID")
	  private FileAttribute fileAttribute;

	  @Column(name = "job_uuid")
	  private String jobUUID;

	  @Column(name = "reportSource")
	  private String reportSource;
	  
	  @Column(name = "status")
	  private String status;
	  
	  @Column(name = "analysis_type")
	  private String analysisType;
	  
	  @Column(name = "score")
	  private int score;
	  
	  @Column(name = "verdict")
	  private String verdict;
	  
	  @Column(name = "sha256")
	  private String sha256;
	  
	  @Column(name = "sanitized")
	  private Boolean sanitized;
	  
	  @Column(name = "staticIndicatorCount")
	  private int staticIndicatorCount;
	  
	  @Column(name = "maliciousActivityCount")
	  private int maliciousActivityCount;

	  @Column(name = "mime_type")
	  private String mimeType;
	  
	  @Column(name = "report_level")
	  private int reportLevel;
	  
	  
	  @Column(name = "submission_timestamp")
	  private Date submissionTimestamp;

	  @CreationTimestamp
	  @Column(name = "timestamp")
	  private Date timestamp;

	@Override
	public String toString() {
		return "SIAnalysis [intellixId=" + intellixId
				+ ", jobUUID=" + jobUUID + ", status=" + status + ", analysisType=" + analysisType + ", score=" + score
				+ ", verdict=" + verdict + ", sha256=" + sha256 + ", staticIndicatorCount=" + staticIndicatorCount
				+ ", maliciousActivityCount=" + maliciousActivityCount + ", mimeType=" + mimeType + ", reportLevel="
				+ reportLevel + ", submissionTimestamp=" + submissionTimestamp + ", timestamp=" + timestamp + "]";
	}

	public Analysis getAnalysis() {
		return analysis;
	}

	public void setAnalysis(Analysis analysis) {
		this.analysis = analysis;
	}

	public FileAttribute getFileAttribute() {
		return fileAttribute;
	}

	public void setFileAttribute(FileAttribute fileAttribute) {
		this.fileAttribute = fileAttribute;
	}

	public String getJobUUID() {
		return jobUUID;
	}

	public void setJobUUID(String jobUUID) {
		this.jobUUID = jobUUID;
	}

	public String getReportSource() {
		return reportSource;
	}

	public void setReportSource(String reportSource) {
		this.reportSource = reportSource;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getAnalysisType() {
		return analysisType;
	}

	public void setAnalysisType(String analysisType) {
		this.analysisType = analysisType;
	}

	public int getScore() {
		return score;
	}

	public void setScore(int score) {
		this.score = score;
	}

	public String getVerdict() {
		return verdict;
	}

	public void setVerdict(String verdict) {
		this.verdict = verdict;
	}

	public String getSha256() {
		return sha256;
	}

	public void setSha256(String sha256) {
		this.sha256 = sha256;
	}

	public Boolean getSanitized() {
		return sanitized;
	}

	public void setSanitized(Boolean sanitized) {
		this.sanitized = sanitized;
	}

	public int getStaticIndicatorCount() {
		return staticIndicatorCount;
	}

	public void setStaticIndicatorCount(int staticIndicatorCount) {
		this.staticIndicatorCount = staticIndicatorCount;
	}

	public int getMaliciousActivityCount() {
		return maliciousActivityCount;
	}

	public void setMaliciousActivityCount(int maliciousActivityCount) {
		this.maliciousActivityCount = maliciousActivityCount;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public int getReportLevel() {
		return reportLevel;
	}

	public void setReportLevel(int reportLevel) {
		this.reportLevel = reportLevel;
	}

	public Date getSubmissionTimestamp() {
		return submissionTimestamp;
	}

	public void setSubmissionTimestamp(Date submissionTimestamp) {
		this.submissionTimestamp = submissionTimestamp;
	}
	
	  public long getIntellixId() {
		return intellixId;
	}

	public Date getTimestamp() {
		return timestamp;
	}

}
