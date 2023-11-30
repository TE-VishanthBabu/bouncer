package com.zorsecyber.bouncer.webapp.dao;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "oAuth2Providers")
public class oAuth2Providers {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PK_Provider_ID")
    private Integer id;

    @Column(name = "Name",length = 32)
    private String name;
}
