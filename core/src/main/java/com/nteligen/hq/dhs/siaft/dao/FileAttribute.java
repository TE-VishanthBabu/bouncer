package com.nteligen.hq.dhs.siaft.dao;

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

import com.zorsecyber.bouncer.core.dao.FileSubmission;
import com.zorsecyber.bouncer.core.dao.SIAnalysis;

import lombok.Data;

@Data
@Entity
@Table(name = "FileAttributes")
public class FileAttribute {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "PK_FileAttributes_ID")
	private long fileAttributeId;
	
	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "FK_Submission_ID")
	private FileSubmission submission;

	@OneToMany(mappedBy = "fileAttribute", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST, orphanRemoval = true)
	private Collection<SIAnalysis> SIAnalyses;
	
//	@Column(name = "FK_Submission_ID")
//	private long submissionId;
	
	@Column(name = "UUID")
	private String originalUuid;

//	@ManyToOne(cascade = CascadeType.ALL)
	@Column(name = "sha256")
	private String sha256;
	
	@Column(name="MD5")
	private String md5;

	@Column(name = "FileName")
	private String fileName;

	@Column(name = "FileType")
	private String fileType;

	@OneToMany(mappedBy = "fileAttribute", cascade = CascadeType.PERSIST, orphanRemoval = true)
	private Collection<Analysis> analyses;

	@OneToMany(mappedBy = "fileAttribute", cascade = CascadeType.PERSIST, orphanRemoval = true)
	private Collection<Sanitize> sanitizes;

	@OneToMany(mappedBy = "fileAttribute", cascade = CascadeType.PERSIST, orphanRemoval = true)
	private Collection<UnprocessedFile> unprocessedFiles;

	@CreationTimestamp
	@Column(name = "Date_Created")
	private Date dateCreated;

	@Column(name = "Date_Updated")
	private Date dateUpdated;

	// omit analysis + sanitize to avoid circular reference
	@Override
	public String toString() {
		return "FileAttribute [fileAttributeId=" + fileAttributeId
				+ ", originalUuid=" + originalUuid + ", sha256=" + sha256 + ", md5=" + md5 + ", fileName=" + fileName
				+ ", fileType=" + fileType 
				+ ", unprocessedFiles=" + unprocessedFiles + ", dateCreated="
				+ dateCreated + ", dateUpdated=" + dateUpdated + "]";
	}
}
