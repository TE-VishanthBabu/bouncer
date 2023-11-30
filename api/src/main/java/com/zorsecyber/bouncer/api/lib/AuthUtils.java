package com.zorsecyber.bouncer.api.lib;

import javax.persistence.EntityManager;

import com.zorsecyber.bouncer.api.dal.BatchDAL;
import com.zorsecyber.bouncer.api.dal.EntityManagerFactorySingleton;
import com.zorsecyber.bouncer.api.dal.UserDAL;
import com.zorsecyber.bouncer.api.dao.Batch;
import com.zorsecyber.bouncer.api.dao.Task;
import com.zorsecyber.bouncer.api.dao.User;


public class AuthUtils {
	private static Object syncLockObj = new Object();

	public AuthUtils() {
	}

	public static Boolean authCredentials(long userId, String secret) throws Exception {
		// verify userId and secret
		User user = UserDAL.getUser(userId);
		if (user == null || user.getSecret() == null) {
			return false;
		}
		return user.getSecret().equals(secret);
	}

	public static <T> Boolean authOwnership(long userId, T obj) throws Exception {

		synchronized (syncLockObj) {
			User user;
			EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
			try {
				user = UserDAL.getUser(userId);
				if (obj.getClass().equals(Batch.class)) {
					return ((Batch) obj).getUser().getUserId() == (user.getUserId());
				}
				else if (obj.getClass().equals(Task.class))
				{
					Batch batch = ((Task) obj).getBatch();
					return batch.getUser().getUserId() == (user.getUserId());
				}
			} catch (Exception ex) {
				throw new Exception("Unable to auth ownership of "+ obj.getClass().getSimpleName() + ": " + obj.toString() +" for user " + "userId");
			} finally {
				if (entityManager != null) {
					entityManager.close();
				}
			}
			return false;
		}
	}
}
