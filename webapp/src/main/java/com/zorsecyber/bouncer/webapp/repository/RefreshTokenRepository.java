package com.zorsecyber.bouncer.webapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.zorsecyber.bouncer.webapp.dao.AccessToken;
import com.zorsecyber.bouncer.webapp.dao.RefreshToken;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken,Integer> {

    /**
     * To get the refresh token details from repo by access tokenId.
     *
     * @param accessTokenId
     * @return refresh token details
     */
    RefreshToken findByAccessToken(AccessToken accessTokenId);
}
