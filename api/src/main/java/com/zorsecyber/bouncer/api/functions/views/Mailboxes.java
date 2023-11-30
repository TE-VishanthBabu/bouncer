package com.zorsecyber.bouncer.api.functions.views;

import java.util.Optional;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.zorsecyber.bouncer.api.dal.SessionDAL;
import com.zorsecyber.bouncer.api.dal.oauth2.MSoAuth2TokenRefresher;
import com.zorsecyber.bouncer.api.dal.oauth2.OAuth2TokenRefresher;
import com.zorsecyber.bouncer.api.dao.Session;
import com.zorsecyber.bouncer.api.dao.User;
import com.zorsecyber.bouncer.api.exceptions.ErrorMessages;
import com.zorsecyber.bouncer.api.exceptions.OAuth2Exception;
import com.zorsecyber.bouncer.api.lib.SessionUtils;
import com.zorsecyber.bouncer.api.lib.msgraph.GraphFunction;
import com.zorsecyber.bouncer.api.lib.reports.PGReportV1;

public class Mailboxes extends GraphFunction {
	@FunctionName("MailboxesView")
	public HttpResponseMessage run(@HttpTrigger(name = "req", methods = {
			HttpMethod.GET }, authLevel = AuthorizationLevel.ANONYMOUS, route = "website/views/graph/mailboxes") HttpRequestMessage<Optional<String>> request,
			final ExecutionContext context)
			throws NumberFormatException, Exception {

		// get jwt from header
		String jwt = getJwt(request, context);
		if(jwt == null)
		{
		request.createResponseBuilder(HttpStatus.UNAUTHORIZED).body(ErrorMessages.wrapErrorMessage("Jwt header not present")).build();
		}
		context.getLogger().info("Got jwt: " + jwt);
		
		// get Session object from jwt
		Session session = SessionDAL.getSession(jwt);
		if(session == null)
		{
			context.getLogger().info("Could not find session for jwt: "+jwt);
			return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body(ErrorMessages.wrapErrorMessage("Session not found")).build();
		}
		// verify session is open
		if (!SessionUtils.sessionIsOpen(session)) {
				return request.createResponseBuilder(HttpStatus.OK).body("Session is closed").build();
		}
		if(!getLengthAndOffset(request))
		{
			return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
					.body(ErrorMessages.wrapErrorMessage("offset and length params are required")).build();
		}

		User user; 
		user = session.getUser();
		context.getLogger().info("got user "+user.getUserId());
		
		accessToken = user.getAccessToken();
		final OAuth2TokenRefresher tokenRefresher;
		try {
			tokenRefresher = new MSoAuth2TokenRefresher();
			if(tokenRefresher.tokenWillExpire(accessToken)) {
				context.getLogger().info("Refreshing tokens");
				tokenRefresher.RefreshAccessToken(accessToken);
			}
			context.getLogger().info("building response");
			return request.createResponseBuilder(HttpStatus.OK).body(PGReportV1.buildMailboxesView(accessToken, length, offset).toString()).build();
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new OAuth2Exception("Unable to refresh tokens", ex);
		}
		
		
	}

}
