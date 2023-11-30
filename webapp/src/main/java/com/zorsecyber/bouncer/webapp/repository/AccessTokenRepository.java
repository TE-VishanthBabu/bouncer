package com.zorsecyber.bouncer.webapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.zorsecyber.bouncer.webapp.dao.AccessToken;
import com.zorsecyber.bouncer.webapp.dao.User;

@Repository
public interface AccessTokenRepository extends JpaRepository<AccessToken,Integer> {
    /**
     * TO get the access token info from repo by userid.
     *
     * @param userId
     * @return access token details
     */
    AccessToken findByUser(User userId);
}
