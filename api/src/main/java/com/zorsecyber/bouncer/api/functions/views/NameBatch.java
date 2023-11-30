package com.zorsecyber.bouncer.api.functions.views;

import java.util.Optional;

import org.json.JSONObject;

import com.microsoft.applicationinsights.core.dependencies.apachecommons.lang3.StringUtils;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.BindingName;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.zorsecyber.bouncer.api.dal.BatchDAL;
import com.zorsecyber.bouncer.api.dal.SessionDAL;
import com.zorsecyber.bouncer.api.dao.Session;
import com.zorsecyber.bouncer.api.dao.User;
import com.zorsecyber.bouncer.api.exceptions.ErrorMessages;
import com.zorsecyber.bouncer.api.lib.AuthUtils;
import com.zorsecyber.bouncer.api.lib.SessionUtils;

public class NameBatch extends WebsiteView {

	@FunctionName("NameBatch")
	public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.PUT},
                authLevel = AuthorizationLevel.ANONYMOUS,
                		route = "website/views/batch/{batchId}/name")
                HttpRequestMessage<Optional<String>> request,
                @BindingName("batchId") String batchId,
            final ExecutionContext context)
	{
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
			context.getLogger().warning("Could not find session for jwt: "+jwt);
			return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body(ErrorMessages.wrapErrorMessage("Session not found")).build();
		}
		// verify session is open
		if (!SessionUtils.sessionIsOpen(session)) {
				return request.createResponseBuilder(HttpStatus.OK).body("Session is closed").build();
		}
		
		String value;
		if(!request.getQueryParameters().containsKey("value")) {
			context.getLogger().warning(("Value param not present"));
			return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body(ErrorMessages.wrapErrorMessage("Value param not present")).build();
		} else {
			value = request.getQueryParameters().get("value");
		}
		
		long _batchId = Long.parseLong(batchId);
		User user = session.getUser();
		context.getLogger().info("Got user: "+user.getUserId());

		// rename batch
		try {
			if(!AuthUtils.authOwnership(user.getUserId(), BatchDAL.getBatch(_batchId))) {
				request.createResponseBuilder(HttpStatus.UNAUTHORIZED).body(ErrorMessages.wrapErrorMessage("User does own batch "+batchId)).build();
			}
			BatchDAL.setBatchName(_batchId, value);
			context.getLogger().info("renamed batch "+batchId);
			return request.createResponseBuilder(HttpStatus.NO_CONTENT).body(StringUtils.EMPTY).build();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body(new JSONObject().put("data", "failed to name batch")).build();
		}
	}

}
