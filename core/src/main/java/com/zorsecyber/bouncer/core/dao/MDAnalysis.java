package com.zorsecyber.bouncer.core.dao;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

import com.nteligen.hq.dhs.siaft.dao.Analysis;
import com.nteligen.hq.dhs.siaft.dao.FileAttribute;

/**
 * This class represents a Metadefender analysis.
 */
@Entity
@Table(name = "Metadefender")
public class MDAnalysis {

	  @Id
	  @GeneratedValue(strategy = GenerationType.AUTO)
	  @Column(name = "PK_MD_ID")
	  private long mdId;

	  @ManyToOne(cascade = CascadeType.ALL)
	  @JoinColumn(name = "FK_AnalysisID")
	  private Analysis analysis;

	  @ManyToOne(cascade = CascadeType.ALL)
	  @JoinColumn(name = "FK_FileAttributeID")
	  private FileAttribute fileAttribute;
	  
	  @Column(name = "dataId")
	  private String dataId;
	  
	  @Column(name = "fileId")
	  private String fileId;
	  
	  @Column(name = "sha256")
	  private String sha256;
	
	  @CreationTimestamp
	  @Column(name = "timestamp")
	  private Date timestamp;
	  
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

		public String getDataId() {
			return dataId;
		}

		public void setDataId(String dataId) {
			this.dataId = dataId;
		}

		public String getFileId() {
			return fileId;
		}

		public void setFileId(String fileId) {
			this.fileId = fileId;
		}

		public String getSha256() {
			return sha256;
		}

		public void setSha256(String sha256) {
			this.sha256 = sha256;
		}

		public long getMdId() {
			return mdId;
		}

		@Override
		public String toString() {
			return "MDAnalysis [mdId=" + mdId + ", analysis=" + analysis + ", fileAttribute=" + fileAttribute
					+ ", dataId=" + dataId + ", fileId=" + fileId + ", sha256=" + sha256 + ", timestamp=" + timestamp
					+ "]";
		}
}
