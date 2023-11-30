package com.zorsecyber.bouncer.api.dal.oauth2;

import org.json.JSONObject;

import lombok.Data;

@Data
public class MSoAuth2TokenRefresherResponse implements OAuth2TokenRefresherResponse {
	private final String accessToken;
    private final String refreshToken;
    private final long expiresIn;
	
	public MSoAuth2TokenRefresherResponse(JSONObject response) {
		accessToken = response.getString("access_token");
	    refreshToken = response.getString("refresh_token");
	    expiresIn = response.getLong("expires_in");
	}

}
