package com.zorsecyber.bouncer.webapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.zorsecyber.bouncer.webapp.dao.Sessions;

@Repository
public interface SessionInfoRepository extends JpaRepository<Sessions,Integer> {
    Sessions findBySessionId(String token);

}
