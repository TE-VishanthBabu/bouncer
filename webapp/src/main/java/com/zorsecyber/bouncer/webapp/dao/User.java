package com.zorsecyber.bouncer.webapp.dao;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashSet;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zorsecyber.bouncer.webapp.constant.ApprovalStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Data
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "Users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "PK_User_ID")
	private Integer userId;

	@Column(name = "Name")
	private String name;

	@Column(name = "first_name")
	private String firstName;

	@Column(name = "last_name")
	private String lastName;

	private String email;

	private String password;

	@Column(name = "Secret")
	private String secret;

	@Column(name = "Creation_Timestamp")
	@CreationTimestamp
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Timestamp creationTimestamp;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ApprovalStatus approvalStatus;

	@ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinTable(name = "User_Roles", joinColumns = @JoinColumn(name = "FK_User_ID", referencedColumnName = "PK_User_ID"), inverseJoinColumns = @JoinColumn(name = "FK_Role_ID", referencedColumnName = "PK_Role_ID"))
	private Collection<Roles> roles = new HashSet<>();

	@ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@Builder.Default
	@JoinColumn(name = "Organization")
	private Organizations organization = null;

	@OneToOne
	@JoinColumn(name = "License")
	@Builder.Default
	@Setter
	private License license = null;

	private String authUserId;
	
	private String apiKey;

	@OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
	private AccessToken accessToken;

	@Override
	public String toString() {
		return "User [userId=" + userId + ", name=" + name + ", secret=" + secret + ", creationTimestamp="
				+ creationTimestamp + "]";
	}

	public void addRole(Roles role) {
		this.roles.add(role);
	}
	
	public void removeRole(Roles role) {
		this.roles.remove(role);
	}
	
	public boolean hasRole(Roles role) {
		return this.roles.contains(role);
	}

	public boolean isLicensed() {
		return license != null;
	}

}
