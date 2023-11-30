package com.zorsecyber.bouncer.api.dal.oauth2;

public interface OAuth2TokenRefresherResponse {
	public String getAccessToken();
	public String getRefreshToken();
	public long getExpiresIn();
}
