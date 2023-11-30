package com.zorsecyber.bouncer.api.functions.views;

import java.util.Optional;

import org.json.JSONObject;

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
import com.zorsecyber.bouncer.api.dao.Batch;
import com.zorsecyber.bouncer.api.dao.Session;
import com.zorsecyber.bouncer.api.dao.User;
import com.zorsecyber.bouncer.api.exceptions.ErrorMessages;
import com.zorsecyber.bouncer.api.lib.AuthUtils;
import com.zorsecyber.bouncer.api.lib.SessionUtils;
import com.zorsecyber.bouncer.api.lib.reports.PGReportV1;

public class TaskView extends WebsiteView {

	@FunctionName("TaskView")
	public HttpResponseMessage run(@HttpTrigger(name = "req", methods = {
			HttpMethod.GET }, authLevel = AuthorizationLevel.ANONYMOUS, route = "website/views/{batchId}/taskView") HttpRequestMessage<Optional<String>> request,
			@BindingName("batchId") String batchId, final ExecutionContext context) {

		// get jwt from header
		String jwt = getJwt(request, context);
		if (jwt == null) {
			request.createResponseBuilder(HttpStatus.UNAUTHORIZED)
					.body(ErrorMessages.wrapErrorMessage("Jwt header not present")).build();
		}
		context.getLogger().info("Got jwt: " + jwt);

		// get Session object from jwt
		Session session = SessionDAL.getSession(jwt);
		if (session == null) {
			context.getLogger().info("Could not find session for jwt: " + jwt);
			return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
					.body(ErrorMessages.wrapErrorMessage("Session not found")).build();
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
		
		orderColumn = "source";
		getOrderColumn(request);
		if(orderColumn.equals("pst"))
			orderColumn = "source";
		else if(orderColumn.equals("total"))
			orderColumn = "numberOfFiles";
		else if(orderColumn.equals("task"))
			orderColumn = "taskId";
		getOrderDirection(request);

		User user = session.getUser();
		context.getLogger().info("Got user: " + user.getUserId());

		Batch batch;
		try {
			batch = BatchDAL.getBatch(Long.parseLong(batchId));
			// make sure the user owns this data
			if (!AuthUtils.authOwnership(user.getUserId(), batch)) {
				return request.createResponseBuilder(HttpStatus.UNAUTHORIZED).body("Unauthorized").build();
			}
		} catch (Exception ex) {
			return request.createResponseBuilder(HttpStatus.NOT_FOUND).body("Batch not found").build();
		}

		try {
			context.getLogger().info(orderColumn);
			PGReportV1 pgReport = new PGReportV1(batch);
			JSONObject report = pgReport.buildTaskView(length, offset, orderColumn, orderDirection);
			return request.createResponseBuilder(HttpStatus.OK).body(report.toString()).build();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed: "+e.getMessage()).build();
		}
	}
}
