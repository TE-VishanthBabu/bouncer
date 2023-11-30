package com.zorsecyber.bouncer.core.dao;

import java.util.Collection;
import java.util.Date;
import java.util.Objects;

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

import com.nteligen.hq.dhs.siaft.dao.FileAttribute;

import lombok.Getter;
import lombok.Setter;


/**
 * This class represents a FileSubmission. A FileSubmission is a file which has been 
 * submitted for processing. Each file submission ID belongs to a Task.
 */
@Getter
@Setter
@Entity
@Table(name = "FileSubmissions")
public class FileSubmission {
	
	@OneToMany(mappedBy = "submission",
			fetch = FetchType.LAZY,
            cascade = CascadeType.PERSIST,
            orphanRemoval = true)
	private Collection<FileAttribute> fileAttributes;
	
	@OneToMany(mappedBy = "submission",
			fetch = FetchType.LAZY,
            cascade = CascadeType.PERSIST,
            orphanRemoval = true)
	private Collection<SIReputation> siReputations;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "PK_Submission_ID")
	private long submissionId;

	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "FK_Task_ID")
	private Task task;
	
	@Column(name = "sha256")
	private String sha256;
	
	@Column(name = "Filename")
	private String filename;
	
	@Column(name= "Success")
	private Boolean success;
	
	@Column(name = "SanitizeStatus")
	private String sanitizeStatus;
	
	@Column(name = "SanitizedVerdict")
	private String sanitizedVerdict;
	
	@Column(name= "Verdict")
	private String verdict;
	
	@CreationTimestamp
	@Column(name = "Timestamp")
	private Date creationTimestamp;

	@Override
	public int hashCode() {
		return Objects.hash(creationTimestamp, filename, sha256, submissionId, success);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FileSubmission other = (FileSubmission) obj;
		return Objects.equals(creationTimestamp, other.creationTimestamp) && Objects.equals(filename, other.filename)
				&& Objects.equals(sha256, other.sha256) && submissionId == other.submissionId
				&& Objects.equals(success, other.success);
	}


}
