package com.zorsecyber.bouncer.api.dal;

import javax.persistence.EntityManager;

import com.zorsecyber.bouncer.api.dao.User;


public class UserDAL {
	private static Object syncLockObj = new Object();

	public UserDAL() {

	}

	public static User getUser(long userId) throws Exception {
		synchronized (syncLockObj) {
			User user;
			EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
			user = entityManager.find(User.class, userId);
			if (entityManager != null) {
				entityManager.close();
			}
			return user;
		}
	}

	public static Boolean verifyUserSecret(long userId, String secret) throws Exception {
		User user = getUser(userId);
		return (user.getSecret().equals(secret));
	}

}
