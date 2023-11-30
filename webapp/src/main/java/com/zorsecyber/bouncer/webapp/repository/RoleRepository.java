package com.zorsecyber.bouncer.webapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.zorsecyber.bouncer.webapp.dao.Roles;

@Repository
public interface RoleRepository extends JpaRepository<Roles, Integer> {
    Roles findByName(String role);
}
