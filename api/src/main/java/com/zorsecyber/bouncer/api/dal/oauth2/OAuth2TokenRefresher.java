package com.zorsecyber.bouncer.api.dal.oauth2;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Properties;
import java.util.TimeZone;

import javax.persistence.EntityManager;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import com.zorsecyber.bouncer.api.dal.EntityManagerFactorySingleton;
import com.zorsecyber.bouncer.api.dao.oauth2.AccessToken;
import com.zorsecyber.bouncer.api.dao.oauth2.RefreshToken;
import com.zorsecyber.bouncer.api.exceptions.OAuth2Exception;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OAuth2TokenRefresher {
	protected static final Object syncLockObj = new Object();
	private static final String oAuth2ConfigFile = "oauth2.properties";
	protected static final long DEFAULT_LOOKAHEAD_MILLIS = 60000;
	protected final String CONTENT_TYPE = "Content-type";
	protected final String ORIGIN = "Origin";
	
	protected final Properties properties;
	
	protected OAuth2TokenRefresher() {
			properties = new Properties();
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			try (InputStream is = loader.getResourceAsStream(oAuth2ConfigFile)) {
			  properties.load(is);
			} catch (IOException ex) {
				log.error("Could not read "+oAuth2ConfigFile);
			}
	}
	
	public AccessToken RefreshAccessToken(AccessToken accessToken) throws Exception {
		return null;
	}
	
	public boolean tokenWillExpire(AccessToken token) {
		return tokenWillExpire(token, DEFAULT_LOOKAHEAD_MILLIS);
	}
	
	public boolean tokenWillExpire(AccessToken token, long lookAheadMillis) {
		long tokenExpiresIn = token.expiresIn();
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		cal.setTimeInMillis(tokenExpiresIn+System.currentTimeMillis());
		log.debug("token expires at "+cal.getTime().toString());
		log.debug("current time "+Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime().toString());
		log.debug(tokenExpiresIn + "<" + lookAheadMillis);
		boolean tokenWillExpire = tokenExpiresIn < lookAheadMillis;
		return tokenWillExpire;
	}
	
	protected AccessToken updateAccessAndRefreshTokens(AccessToken accessToken, OAuth2TokenRefresherResponse response) throws OAuth2Exception {
		String newAccessToken = response.getAccessToken();
		String newRefreshToken = response.getRefreshToken();
		long expiresIn = response.getExpiresIn();
		// access token expiry
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Timestamp accessTokenExpiry = new Timestamp(System.currentTimeMillis() + (expiresIn * 1000));
        // refresh token expiry
        Calendar refreshTokenCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        if(newRefreshToken != null) {
        	refreshTokenCalendar.add(Calendar.DAY_OF_MONTH, 14);
        }
		EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
		AccessToken accessTokenEntity = null;
		synchronized (syncLockObj) {
			try {
				entityManager.getTransaction().begin();
				accessTokenEntity = entityManager.find(AccessToken.class, accessToken.getId());

				accessTokenEntity.setAccessToken(newAccessToken);
				accessTokenEntity.setExpires(accessTokenExpiry);
				RefreshToken refreshToken = accessTokenEntity.getRefreshToken();
				refreshToken.setRefreshToken(newRefreshToken);
				refreshToken.setExpires(new Timestamp(refreshTokenCalendar.getTimeInMillis()));
				entityManager.persist(accessTokenEntity);
				entityManager.persist(refreshToken);
				entityManager.flush();
				entityManager.getTransaction().commit();

				return accessTokenEntity;

			} catch (Exception ex) {
				if (entityManager != null && entityManager.getTransaction().isActive()) {
					entityManager.getTransaction().rollback();
				}
				if(accessTokenEntity != null)
					throw new OAuth2Exception("Unable to update tokens for user" + accessTokenEntity.getUser().getUserId() + " provider " + accessTokenEntity.getProvider().toString(), ex);
				else
					throw new OAuth2Exception("Unable to retrieve token entities for accessToken " + accessToken.getId(), ex);
			} finally {
				if (entityManager != null) {
					entityManager.close();
				}
			}
		}
	}
	
	protected JSONObject postRequest(String url, HttpEntity entity) throws URISyntaxException
	{
		URIBuilder uriParams = new URIBuilder(url);

		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpPost httpPost = new HttpPost(uriParams.build());
		httpPost.setHeader(CONTENT_TYPE, "application/x-www-form-urlencoded");
		httpPost.setHeader(ORIGIN, "http://localhost:8081");

		if(entity != null) {
			httpPost.setEntity(entity);
		}

		String responseBody = null;
		try (CloseableHttpResponse proxyResponse = httpclient.execute(httpPost))
		{
		HttpEntity responseEntity = proxyResponse.getEntity();
		responseBody = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);

		JSONObject jsonResponse = new JSONObject(responseBody);
		return jsonResponse;
		}
		catch(Exception e)
		{
			JSONObject error = new JSONObject()
					.put("error",
							new JSONObject()
							.put("message", e.getMessage())
							.put("responsebody", responseBody));
			return error;
		}
	}
}
