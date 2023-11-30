package com.zorsecyber.bouncer.api.functions.timerfunctions;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.TimerTrigger;
import com.zorsecyber.bouncer.api.dal.EntityManagerFactorySingleton;
import com.zorsecyber.bouncer.api.dal.oauth2.MSoAuth2TokenRefresher;
import com.zorsecyber.bouncer.api.dal.oauth2.OAuth2TokenRefresher;
import com.zorsecyber.bouncer.api.dao.User;
import com.zorsecyber.bouncer.api.dao.oauth2.AccessToken;
import com.zorsecyber.bouncer.api.exceptions.OAuth2Exception;

public class TokenRefresher {
	private static Object syncLockObj = new Object();
	
	// trigger every 4 days
	@FunctionName("TokenRefresher")
	public void run(@TimerTrigger(name = "TokenRefreshTrigger", schedule = "0 0 0 * * */4") String timerInfo,
			ExecutionContext context) {
		// get all users with linked microsoft accounts
		EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
		synchronized (syncLockObj) {
			try {
				String usersQuery = "select u.accessToken from User u where u.accessToken is not null";
				Query query = entityManager.createQuery(usersQuery);
				@SuppressWarnings("unchecked")
				List<AccessToken> accessTokens = query.getResultList();
				final OAuth2TokenRefresher tokenRefresher = new MSoAuth2TokenRefresher();
				User user;
				for(AccessToken accessToken : accessTokens) {
					user = accessToken.getUser();
					context.getLogger().info("Refreshing tokens for "+user.getEmail());
					tokenRefresher.tokenWillExpire(accessToken);
					try {
						tokenRefresher.RefreshAccessToken(accessToken);
					} catch(OAuth2Exception e) {
						context.getLogger().severe("Unable to refresh tokens for "+user.getEmail()+": "+e.getMessage());
					}
					context.getLogger().info("Tokens refreshed for "+user.getEmail());
				}
			} catch (Exception ex) {
				if (entityManager != null && entityManager.getTransaction().isActive()) {
					entityManager.getTransaction().rollback();
				}
				ex.printStackTrace();
			} finally {
				if (entityManager != null) {
					entityManager.close();
				}
			}
		}
	}

}
