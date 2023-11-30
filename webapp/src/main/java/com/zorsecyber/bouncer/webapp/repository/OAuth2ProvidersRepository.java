package com.zorsecyber.bouncer.webapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.zorsecyber.bouncer.webapp.dao.oAuth2Providers;

@Repository
public interface OAuth2ProvidersRepository extends JpaRepository<oAuth2Providers,Integer> {
	public oAuth2Providers findByName(String name);
}
