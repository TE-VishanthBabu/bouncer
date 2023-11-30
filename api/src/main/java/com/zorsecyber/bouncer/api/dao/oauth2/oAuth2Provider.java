package com.zorsecyber.bouncer.api.dao.oauth2;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "oAuth2Providers")
public class oAuth2Provider {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "PK_Provider_ID")
    private Integer id;

    @Column(name = "Name",length = 32)
    private String name;
}
