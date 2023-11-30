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

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "Sessions")
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "PK_Session_ID")
    private Integer id;

    @Column(name = "sessionId",
    		length = 1500)
    private String sessionId;

    @Column(name="Creation_Timestamp")
    @CreationTimestamp
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date creationTimestamp;

	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "FK_User_ID")
	private User user;
}