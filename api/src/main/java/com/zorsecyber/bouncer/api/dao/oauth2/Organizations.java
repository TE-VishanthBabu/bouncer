package com.zorsecyber.bouncer.api.dao.oauth2;

import java.util.Collection;
import java.util.HashSet;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.zorsecyber.bouncer.api.dao.License;
import com.zorsecyber.bouncer.api.dao.User;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "Organizations")
public class Organizations {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PK_Organization_ID")
    private Integer id;

    @Column(name = "OrganizationName")
    private String organizationName;
    
    
	@OneToMany(mappedBy = "organization",
			fetch = FetchType.LAZY,
            cascade = CascadeType.PERSIST)
	private Collection<User> users = new HashSet<>();
	
}
