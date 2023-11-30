package com.zorsecyber.bouncer.api.dao;

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

/**
 * This class represents an siIndicator
 */
@Getter
@Setter
@Entity
@Table(name = "SIIndicators")
public class SIIndicator {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "PK_SIIndicator_ID")
	private long siIndicatorId;
	
	@ManyToOne(cascade = CascadeType.MERGE)
	@JoinColumn(name = "FK_Intelix_ID")
	private SIAnalysis siAnalysis;	// also known as the Intelix ID
	
	@Column(name="severity")
	private long severity;
	
	@Column(name="name")
	private String name;
	
	@Column(name="description")
	private String description;
	
	@Column(name="timestamp")
	  private Date timestamp;
	
	@Override
	public String toString() {
		return "SIIndicator [siIndicatorId=" + siIndicatorId + ", siAnalysis=" + siAnalysis + ", severity=" + severity
				+ ", name=" + name + ", description=" + description + ", timestamp=" + timestamp + "]";
	}
}
