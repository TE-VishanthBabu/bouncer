package com.zorsecyber.bouncer.webapp.dao;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "AccessToken")
public class AccessToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PK_AccessToken_ID")
    private Integer id;

    @Column(name = "AccessToken",length = 3000)
    private String accessToken;
    
    @Column(name="Expires")
    private Timestamp expires;

    @UpdateTimestamp
    @Column(name="Update_Timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp updateTimestamp;

    @OneToOne
    @JoinColumn(name = "FK_User_ID",referencedColumnName = "PK_User_ID")
    private User user;

    @OneToOne
    @JoinColumn(name = "FK_Provider_ID",referencedColumnName = "PK_Provider_ID")
    private oAuth2Providers providers;
}
