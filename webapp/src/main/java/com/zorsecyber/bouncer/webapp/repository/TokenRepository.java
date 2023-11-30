package com.zorsecyber.bouncer.webapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.zorsecyber.bouncer.webapp.dao.Token;

@Repository
public interface TokenRepository extends JpaRepository<Token, Integer> {

    Token findByUserId(Integer userId);
    Token findByEmailVerificationToken(String token);
    Token findByForgotPasswordToken(String token);
}
