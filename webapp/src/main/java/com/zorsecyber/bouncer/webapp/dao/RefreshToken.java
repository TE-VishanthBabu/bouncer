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
@Table(name = "RefreshToken")
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PK_RefreshToken_ID")
    private Integer id;

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
