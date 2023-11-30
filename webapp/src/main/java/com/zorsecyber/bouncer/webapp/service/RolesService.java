package com.zorsecyber.bouncer.webapp.service;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import com.zorsecyber.bouncer.webapp.dao.Roles;
import com.zorsecyber.bouncer.webapp.dao.User;
import com.zorsecyber.bouncer.webapp.repository.RoleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class RolesService {
	private final RoleRepository roleRepository;

	/**
	 * Assign user roles based on their license
	 * 
	 * @param user
	 */
	public void assignRolesBasedOnLicense(User user) {
		String roleToAssign = "ROLE_UNLICENSED";
		String roleToRemove = null;
		if (user.isLicensed()) {
			roleToRemove = "ROLE_UNLICENSED";
			roleToAssign = "ROLE_USER";
		} else {
			roleToRemove = "ROLE_USER";
		}
		Roles role = roleRepository.findByName(roleToRemove);
		if (!ObjectUtils.isEmpty(role) && user.hasRole(role)) {
			user.removeRole(role);
			log.info("revoked role " + role.getName() + " from user " + user.getEmail());
		}
		role = roleRepository.findByName(roleToAssign);
		if (ObjectUtils.isEmpty(role)) {
			role = new Roles(roleToAssign);
			roleRepository.save(role);
		}
		if (!user.hasRole(role)) {
			user.addRole(role);
			log.info("assigned role " + role.getName() + " from user " + user.getEmail());
		}
	}

}
