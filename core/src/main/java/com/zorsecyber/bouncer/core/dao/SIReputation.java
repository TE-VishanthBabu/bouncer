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

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "SIReputation")
public class SIReputation {
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "PK_Reputation_ID")
	private long reputationId;
	
	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "FK_Submission_ID")
	private FileSubmission submission;
	
	@Column(name = "first_seen")
	private Date firstSeen;
	
	@Column(name = "last_seen")
	private Date lastSeen;
	
	@Column(name = "prevalence")
	private String prevalence;
	
	@Column(name = "score")
	private int score;
	
	@Column(name = "score_string")
	private String scoreString;

}
