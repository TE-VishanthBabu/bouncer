package com.zorsecyber.bouncer.api.functions.views;

import java.util.Optional;

import org.json.JSONObject;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.zorsecyber.bouncer.api.dal.SessionDAL;
import com.zorsecyber.bouncer.api.dao.Session;
import com.zorsecyber.bouncer.api.dao.User;
import com.zorsecyber.bouncer.api.exceptions.ErrorMessages;
import com.zorsecyber.bouncer.api.lib.SessionUtils;
import com.zorsecyber.bouncer.api.lib.reports.PGReportV1;

public class BatchView extends WebsiteView {
	
	@FunctionName("BatchView")
	public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET},
                authLevel = AuthorizationLevel.ANONYMOUS,
                		route = "website/views/batchView")
                HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context)
	{
		// get jwt from header
		String jwt = getJwt(request, context);
		if(jwt == null)
		{
		request.createResponseBuilder(HttpStatus.UNAUTHORIZED).body(ErrorMessages.wrapErrorMessage("Jwt header not present")).build();
		}
		context.getLogger().fine("Got jwt: " + jwt);
		
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
		orderColumn = "batchId";
		getOrderDirection(request);
		context.getLogger().fine("Direction: "+orderDirection);
		
		User user = session.getUser();
		context.getLogger().fine("Got user: "+user.getUserId());

		// build batch view
		try {
			PGReportV1 pgReport = new PGReportV1(null);
			JSONObject report = pgReport.buildBatchView(user, length, offset, orderColumn, orderDirection);
			return request.createResponseBuilder(HttpStatus.OK).body(report.toString()).build();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return request.createResponseBuilder(HttpStatus.OK).body("Failed").build();
		}
	}
}
