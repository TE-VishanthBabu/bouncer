package com.zorsecyber.bouncer.webapp.service;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import com.zorsecyber.bouncer.webapp.dao.Organizations;
import com.zorsecyber.bouncer.webapp.dao.User;
import com.zorsecyber.bouncer.webapp.repository.OrganizationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class OrganizationsService {
	private final OrganizationRepository organizationRepository;
	
	/**
	 * Saves the user's organization and assigns it to the user
	 * 
	 * @param user
	 * @return Organization
	 */
	public Organizations saveAndUpdateUserOrganization(User user) {
		Organizations org = saveOrganization(user.getEmail());
		user.setOrganization(org);
		return org;
	}
	
	/**
	 * Save organization details.
	 *
	 * @param userEmail
	 * @return organizations
	 */
	public Organizations saveOrganization(String userEmail) {
		Organizations org;
		String[] parts = userEmail.split("@");
		if (ObjectUtils.isEmpty(org = organizationRepository.findByOrganizationName(parts[1]))) {
			log.info("creating org "+parts[1]);
			org = new Organizations();
			org.setOrganizationName(parts[1]);
			return organizationRepository.save(org);
		}
		return org;

	}
}
