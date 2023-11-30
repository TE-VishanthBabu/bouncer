package com.zorsecyber.bouncer.api.dao.oauth2;

import java.sql.Timestamp;

public interface OAuth2Token {
	public static final long DEFAULT_ACCESS_TOKEN_LIFETIME = 3600 * 1000; // one hour
	public static final long DEFAULT_REFRESH_TOKEN_LIFETIME = 14 * 24 * 3600 * 1000; // two weeks
	
	public Timestamp getExpires();

}
