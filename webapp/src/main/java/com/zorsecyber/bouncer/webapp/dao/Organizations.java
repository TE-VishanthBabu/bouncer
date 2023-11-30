package com.zorsecyber.bouncer.webapp.dao;

import java.util.Collection;
import java.util.HashSet;


import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
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
    private Integer organizationId;

    @Column(name = "OrganizationName")
    private String organizationName;
    
	@OneToMany(mappedBy = "organization",
			fetch = FetchType.LAZY,
            cascade = CascadeType.PERSIST)
	private Collection<User> users = new HashSet<>();
	
	@OneToOne(mappedBy = "organization")
	private License license;
	
	@Override
	public String toString() {
		return organizationName;
		
	}
}
