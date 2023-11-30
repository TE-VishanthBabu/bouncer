package com.zorsecyber.bouncer.webapp.service;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import com.zorsecyber.bouncer.webapp.dao.License;
import com.zorsecyber.bouncer.webapp.dao.User;
import com.zorsecyber.bouncer.webapp.repository.LicenseRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class LicenseService {
	private final LicenseRepository licenseRepository;
	
	/**
	 * looks up license for user using email and organization. Applies
	 * license if it exists or revokes license if it has been revoked
	 * 
	 * @param user
	 */
	public void lookupAndAssignLicense(User user) {
		License license;
		if(ObjectUtils.isEmpty(license = licenseRepository.findByUserEmailOrOrganization(user.getEmail(), user.getOrganization()))) {
			license = null;
		}
		user.setLicense(license);
	}
}
