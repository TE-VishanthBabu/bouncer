package com.zorsecyber.bouncer.webapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.zorsecyber.bouncer.webapp.dao.License;
import com.zorsecyber.bouncer.webapp.dao.Organizations;

@Repository
public interface LicenseRepository extends JpaRepository<License,Integer> {
    /**
     * get the license info from repo by user email or organization.
     *
     * @param email
     * @param org
     * @return License details
     */
    License findByUserEmailOrOrganization(String email, Organizations org);
    
    License findByUserEmail(String email);
    
    License findByOrganization(Organizations org);
}
