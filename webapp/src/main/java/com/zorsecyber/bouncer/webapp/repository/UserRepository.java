package com.zorsecyber.bouncer.webapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.zorsecyber.bouncer.webapp.constant.ApprovalStatus;
import com.zorsecyber.bouncer.webapp.dao.User;


@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    /**
     * Getting user info from repository by email and approval status.
     *
     * @param email
     * @return user
     */
    User findByEmailAndApprovalStatusEquals(String email, ApprovalStatus status);

    /**
     * Getting user info from repository by email.
     *
     * @param email
     * @return user
     */
    User findByEmail(String email);

    /**
     * Getting user info from repository by email and oAuthId.
     *
     * @param email
     * @param approvalStatus
     * @param authId
     * @return user
     */

    User findByEmailAndApprovalStatusEqualsAndAuthUserId(String email,ApprovalStatus approvalStatus,String authId);
}
