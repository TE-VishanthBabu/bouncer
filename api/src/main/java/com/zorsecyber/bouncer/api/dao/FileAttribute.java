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
@Table(name = "FileAttributes")
public class FileAttribute {
	@OneToMany(mappedBy = "fileAttribute",
			fetch = FetchType.LAZY,
			cascade = CascadeType.PERSIST,
            orphanRemoval = true)
	private Collection<SIAnalysis> siAnalyses;
	
	@OneToMany(mappedBy = "fileAttribute", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST, orphanRemoval = true)
	private Collection<Sanitize> sanitizes;
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "PK_FileAttributes_ID")
	private long fileAttributeId;

	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "FK_Submission_ID")
	private FileSubmission submission;
	
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

	@CreationTimestamp
	@Column(name = "Date_Created")
	private Date dateCreated;

	@Column(name = "Date_Updated")
	private Date dateUpdated;

}