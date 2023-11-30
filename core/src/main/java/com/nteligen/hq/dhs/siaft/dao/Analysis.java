package com.nteligen.hq.dhs.siaft.dao;

import java.util.Collection;
import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

import com.zorsecyber.bouncer.core.dao.SIAnalysis;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This class represents an Analysis by an analysis engine. This can also be associated with a
 * sanitization as well but if a file were analyzed before any sanitization engine then the
 * sanitization engine would be null.
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "Analysis")
public class Analysis
{
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "PK_Analysis_ID")
  private long analysisId;
  
  @OneToMany(mappedBy="analysis",
	  		cascade = CascadeType.ALL)
  private Collection<SIAnalysis> SIAnalyses;

  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "FK_FileAttributes_ID")
  private FileAttribute fileAttribute;

  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "FK_AnalysisEngineID")
  private AnalyzeEngine analyzeEngine;

  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "FK_SanitizeEngineID")
  private SanitizeEngine sanitizeEngine;

  @Column(name = "Results")
  private String results;

  @Column(name = "Success")
  private boolean success;

  @CreationTimestamp
  @Column(name = "Date_Created")
  private Date dateCreated;

  @Column(name = "Date_Updated")
  private Date dateUpdated;

  /**
   * This will instantate a new Analysis with the passed in information.
   * @param fileAttribute the associated fileAttribute object
   * @param analyzeEngine the associated analyzeEngine
   * @param success Whether the analysis was successful or not
   */
  public Analysis(FileAttribute fileAttribute, AnalyzeEngine analyzeEngine, boolean success)
  {
    this.fileAttribute = fileAttribute;
    this.analyzeEngine = analyzeEngine;
    this.success = success;
  }

  /**
   * This will instantate a new Analysis with the passed in information.
   * @param fileAttribute the associated fileAttribute object
   * @param analyzeEngine the associated analyzeEngine
   * @param sanitizeEngine the assocaited sanitizeEngine
   * @param success Whether the analysis was successful or not
   */
  public Analysis(FileAttribute fileAttribute,
                  AnalyzeEngine analyzeEngine,
                  SanitizeEngine sanitizeEngine,
                  boolean success)
  {
    this.fileAttribute = fileAttribute;
    this.analyzeEngine = analyzeEngine;
    this.sanitizeEngine = sanitizeEngine;
    this.success = success;
  }
  
  @Override
public String toString() {
	return "Analysis [analysisId=" + analysisId + ", SIAnalyses=" + SIAnalyses + ", fileAttribute=" + fileAttribute
			+ ", analyzeEngine=" + analyzeEngine + ", sanitizeEngine=" + sanitizeEngine + ", results=" + results
			+ ", success=" + success + ", dateCreated=" + dateCreated + ", dateUpdated=" + dateUpdated + "]";
} 
}
