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

/**
 * This class represents an siIndicator
 */
@Entity
@Table(name = "SIIndicators")
public class SIIndicator {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "PK_SIIndicator_ID")
	private long siIndicatorId;
	
	@ManyToOne(cascade = CascadeType.MERGE)
	@JoinColumn(name = "FK_Intelix_ID")
	private SIAnalysis siAnalysis;
	
	@Column(name="severity")
	private long severity;
	
	@Column(name="name")
	private String name;
	
	@Column(name="description")
	private String description;
	
	@Column(name="timestamp")
	  private Date timestamp;
	
	public long getSeverity() {
		return severity;
	}
	public void setSeverity(long severity) {
		this.severity = severity;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String descrption) {
		this.description = descrption;
	}
	public SIAnalysis getSiAnalysis() {
		return siAnalysis;
	}
	public void setSiAnalysis(SIAnalysis siAnalysis) {
		this.siAnalysis = siAnalysis;
	}
	@Override
	public String toString() {
		return "SIIndicator [siIndicatorId=" + siIndicatorId + ", siAnalysis=" + siAnalysis + ", severity=" + severity
				+ ", name=" + name + ", description=" + description + ", timestamp=" + timestamp + "]";
	}
	
}
