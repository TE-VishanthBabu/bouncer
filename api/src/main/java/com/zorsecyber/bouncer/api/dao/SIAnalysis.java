package com.zorsecyber.bouncer.api.dao;

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

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "Intellix")
public class SIAnalysis
{

	  @Id
	  @GeneratedValue(strategy = GenerationType.AUTO)
	  @Column(name = "PK_IntellixID")
	  private long intellixId;
	  
	  @OneToMany(fetch = FetchType.LAZY,
			  	mappedBy = "siAnalysis",
	             cascade = CascadeType.PERSIST,
	             orphanRemoval = true)
	  private Collection<SIIndicator> siIndicators;

//	  @ManyToOne(cascade = CascadeType.ALL)
//	  @JoinColumn(name = "FK_AnalysisID")
//	  private Analysis analysis;

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
	  
	  @CreationTimestamp
	  @Column(name = "submission_timestamp")
	  private Date submissionTimestamp;

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

}
