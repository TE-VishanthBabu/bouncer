package com.zorsecyber.bouncer.api.dao;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.zorsecyber.bouncer.api.dao.oauth2.AccessToken;
import com.zorsecyber.bouncer.api.dao.oauth2.Organizations;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "Users")
public class User {
	
	  @Id
	  @GeneratedValue(strategy = GenerationType.AUTO)
	  @Column(name = "PK_User_ID")
	  private long userId;
	  
	  @OneToMany(mappedBy = "user",
			  	 fetch = FetchType.LAZY,
	             cascade = CascadeType.PERSIST,
	             orphanRemoval = true)
	  private Collection<Batch> batches;
	  
	  @OneToMany(mappedBy = "user",
			  	 fetch = FetchType.LAZY,
	             cascade = CascadeType.PERSIST,
	             orphanRemoval = true)
	  private Collection<Session> sessions;
	  
	  @OneToOne(mappedBy = "user")
	  private AccessToken accessToken;
	  
	  @Column(name="Name")
	  private String name;
	  
	  @Column(name="email")
	  private String email;
	  
	  @Column(name="Secret")
	  private String secret;
	  
		@ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
		@JoinColumn(name = "FK_Organization_ID")
		private Organizations organization = null;

		@OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
		private License license = null;
	  
	  @Column(name="Creation_Timestamp")
	  private Date creationTimestamp;
	
	public Set<Batch> getUniqueBatches()
	{
		return new LinkedHashSet<Batch>(getBatches());
	}

	@Override
	public String toString() {
		return "User [userId=" + userId + ", name=" + name + ", secret=" + secret
				+ ", creationTimestamp=" + creationTimestamp + "]";
	}
	  
}
