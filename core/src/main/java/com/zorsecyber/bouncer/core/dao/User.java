package com.zorsecyber.bouncer.core.dao;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * This class represents User. Users have an ID, name and secret
 */
@Entity
@Table(name = "Users")
public class User {
	
	  @Id
	  @GeneratedValue(strategy = GenerationType.AUTO)
	  @Column(name = "PK_User_ID")
	  private long userId;

	  @Column(name="Name")
	  private String name;
	  
	  @Column(name="Secret")
	  private String secret;
	  
	  @Column(name="Creation_Timestamp")
	  private Date creationTimestamp;
}
