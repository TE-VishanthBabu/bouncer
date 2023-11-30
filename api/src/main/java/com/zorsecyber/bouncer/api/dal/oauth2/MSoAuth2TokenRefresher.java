package com.zorsecyber.bouncer.api.dal.oauth2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import com.zorsecyber.bouncer.api.dao.User;
import com.zorsecyber.bouncer.api.dao.oauth2.AccessToken;
import com.zorsecyber.bouncer.api.dao.oauth2.RefreshToken;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MSoAuth2TokenRefresher extends OAuth2TokenRefresher {

	public MSoAuth2TokenRefresher() throws IOException {
		super();
	}
	
	@Override
	public AccessToken RefreshAccessToken(AccessToken accessToken) throws Exception {
		RefreshToken refreshToken = accessToken.getRefreshToken();
	    String url = "https://login.microsoftonline.com/common/oauth2/v2.0/token";

	    List<NameValuePair> params = new ArrayList<NameValuePair>();
	    params.add(new BasicNameValuePair("client_id", properties.getProperty("ms.app.clientId")));
	    params.add(new BasicNameValuePair("refresh_token", refreshToken.getRefreshToken()));
	    params.add(new BasicNameValuePair("grant_type", "refresh_token"));
	    UrlEncodedFormEntity form = new UrlEncodedFormEntity(params);
	    JSONObject response = postRequest(url, form);
	    OAuth2TokenRefresherResponse tokenResponse = new MSoAuth2TokenRefresherResponse(response);
	    
        return updateAccessAndRefreshTokens(accessToken, tokenResponse);
	}
}
