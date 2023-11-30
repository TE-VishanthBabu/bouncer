package com.zorsecyber.bouncer.webapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.zorsecyber.bouncer.webapp.dao.Organizations;

@Repository
public interface OrganizationRepository extends JpaRepository<Organizations,Integer> {
	public Organizations findByOrganizationName(String name);
}
