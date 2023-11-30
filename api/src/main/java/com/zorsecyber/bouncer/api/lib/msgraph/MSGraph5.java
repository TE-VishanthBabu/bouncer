package com.zorsecyber.bouncer.api.lib.msgraph;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.microsoft.graph.httpcore.HttpClients;
import com.microsoft.graph.logger.LoggerLevel;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.UserCollectionPage;
import com.zorsecyber.bouncer.api.exceptions.MSGraphException;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;

@Slf4j
@SuppressWarnings("null")
public class MSGraph5 {

	private String accessToken;
	private final OkHttpClient httpClient;
	@Getter
	private final GraphServiceClient<Request> graphServiceClient;

	@SuppressWarnings("null")
	public MSGraph5(String token) {
		accessToken = token;
		NullLogger nullLogger = new NullLogger();
		nullLogger.setLoggingLevel(LoggerLevel.ERROR);
		CustomMSGraphAccessTokenAuthProvider tokenCredAuthProvider = new CustomMSGraphAccessTokenAuthProvider(accessToken);
			httpClient = HttpClients.createDefault(tokenCredAuthProvider)
					.newBuilder()
					.followSslRedirects(false)
					.readTimeout(Duration.ofMinutes(1))
					.build();
			graphServiceClient = GraphServiceClient
					.builder()
					.httpClient(httpClient)
					.logger(nullLogger)
					.buildClient();
	}

	public List<String> getMailboxes(int offset, int length) {
		List<String> mbs = new ArrayList<String>();
		UserCollectionPage users = graphServiceClient.users().buildRequest().select("mail").get();
		int count = 1;
		while (users != null) {
			for (User user : users.getCurrentPage()) {
				if (count > offset && user.mail != null)
					mbs.add(user.mail.toLowerCase());
				count++;
			}
			if (users.getNextPage() == null || (count > offset + length && length != -1)) {
				users = null;
			} else {
				users = users.getNextPage().buildRequest().get();
			}
		}
		return mbs;
	}
	
	public boolean canGraphUserAccessMailbox(String mailbox) {
		try {
		graphServiceClient
				.users(mailbox)
				.messages()
				.buildRequest()
				.select("id")
				.top(1)
				.get();
		return true;
		} catch (Exception ex) {
			log.warn(ex.getMessage());
			return false;
		}
	}
}
