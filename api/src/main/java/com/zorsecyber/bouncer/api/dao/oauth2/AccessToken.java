package com.zorsecyber.bouncer.api.dao.oauth2;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.TimeZone;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zorsecyber.bouncer.api.dao.User;

import lombok.Data;

@Data
@Entity
@Table(name = "AccessToken")
public class AccessToken implements OAuth2Token {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "PK_AccessToken_ID")
    private long id;

    @Column(name = "AccessToken",length = 3000)
    private String accessToken;
    
    @Column(name="Expires")
    private Timestamp expires;

    @UpdateTimestamp
    @Column(name="Update_Timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp updateTimestamp;
    
    @OneToOne(mappedBy = "accessToken")
	private RefreshToken refreshToken;

    @OneToOne
    @JoinColumn(name = "FK_User_ID",referencedColumnName = "PK_User_ID")
    private User user;

    @OneToOne
    @JoinColumn(name = "FK_Provider_ID",referencedColumnName = "PK_Provider_ID")
    private oAuth2Provider provider;
    
    public long expiresIn() {
		return expires.getTime() - System.currentTimeMillis();
    }
}