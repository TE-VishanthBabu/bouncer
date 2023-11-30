package com.zorsecyber.bouncer.api.lib.msgraph;

import java.net.URL;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nonnull;

import com.microsoft.graph.authentication.IAuthenticationProvider;

public class CustomMSGraphAccessTokenAuthProvider implements IAuthenticationProvider {

    private String accessToken;

    public CustomMSGraphAccessTokenAuthProvider(String accessToken) {
        this.accessToken = accessToken;
    }

	@SuppressWarnings("null")
	@Override
	@Nonnull
	public CompletableFuture<String> getAuthorizationTokenAsync(@Nonnull URL requestUrl) {
		// TODO Auto-generated method stub
		return CompletableFuture.completedFuture(accessToken);
	}
}
