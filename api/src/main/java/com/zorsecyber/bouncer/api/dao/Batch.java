package com.zorsecyber.bouncer.api.dao;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

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
@Table(name = "Batches")
public class Batch {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "PK_Batch_ID")
	private long batchId;

	@OneToMany(fetch = FetchType.LAZY,
			  	mappedBy = "batch",
	             cascade = CascadeType.PERSIST,
	             orphanRemoval = true)
	  private Collection<Task> tasks;
	
	@ManyToOne(cascade = CascadeType.PERSIST)
	@JoinColumn(name = "FK_User_ID")
	private User user;
	
	@Column(name = "Name")
	private String name;

	@CreationTimestamp
	@Column(name = "Timestamp")
	private Date creationTimestamp;
	
	public Set<Task> getUniqueTasks()
	{
		return new LinkedHashSet<Task>(getTasks());
	}
}
