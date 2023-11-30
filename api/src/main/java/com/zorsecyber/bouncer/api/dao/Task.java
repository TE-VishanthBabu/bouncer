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
import org.json.JSONObject;

import com.zorsecyber.bouncer.api.lib.status.TaskStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "Tasks")
public class Task {

	@OneToMany(mappedBy = "task",
			fetch = FetchType.LAZY,
            cascade = CascadeType.PERSIST,
            orphanRemoval = true)
 private Collection<FileSubmission> fileSubmissions;
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "PK_Task_ID")
	private long taskId;

	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "FK_Batch_ID")
	private Batch batch;

	@Column(name = "Source")
	private String source;
	
	@Column(name = "Sanitize")
	private boolean sanitize;
	
	@Column(name = "NumberOfFiles")
	private int numberOfFiles;
	
	@Column(name = "FilesCompleted")
	private int filesCompleted;
	
	@Column(name = "Size")
	private float size;

	@Column(name = "Status")
	private TaskStatus status;
	
	@Column(name = "Message")
	private String message;
	
	@Column(name= "Clean")
	private int clean;
	
	@Column(name= "Suspicious")
	private int suspicious;
	
	@Column(name= "Malicious")
	private int malicious;

	@CreationTimestamp
	@Column(name = "Timestamp")
	private Date creationTimestamp;

	@Override
	public String toString() {
		return "Task [taskId=" + taskId + ", source=" + source + ", status=" + status
				+ ", creationTimestamp=" + creationTimestamp + "]";
	}

	public JSONObject toJson()
	{
		JSONObject task = new JSONObject();
		task.append("taskId", this.getTaskId());
		task.append("batchId", this.getBatch().getBatchId());
		task.append("source", this.getSource());
		task.append("status", this.getStatus());
		task.append("timestamp", this.getCreationTimestamp());
		return task;
	}
	
	public boolean isComplete() {
			int filesCompleted = getFilesCompleted();
			int numberOfFiles = getNumberOfFiles();
			return getStatus() == TaskStatus.PROCESSING && (filesCompleted >= numberOfFiles && numberOfFiles != 0) || numberOfFiles == -1;
	}

}
