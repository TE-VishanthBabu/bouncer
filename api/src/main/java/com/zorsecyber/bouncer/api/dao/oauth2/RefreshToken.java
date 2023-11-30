package com.zorsecyber.bouncer.api.dao.oauth2;

import java.sql.Timestamp;

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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "RefreshToken")
public class RefreshToken implements OAuth2Token {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "PK_RefreshToken_ID")
    private long id;

    @Column(name = "RefreshToken",length = 3000)
    private String refreshToken;

    @Column(name="Expires")
    private Timestamp expires;

    @UpdateTimestamp
    @Column(name="Update_Timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp updateTimestamp;

    @OneToOne
    @JoinColumn(name = "FK_AccessToken_ID",referencedColumnName = "PK_AccessToken_ID")
    private AccessToken accessToken;

}