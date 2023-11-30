package com.zorsecyber.bouncer.api.functions.submit;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONObject;

import com.azure.storage.queue.QueueClient;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.zorsecyber.bouncer.api.dal.BatchDAL;
import com.zorsecyber.bouncer.api.dal.QueueDAL;
import com.zorsecyber.bouncer.api.dal.SessionDAL;
import com.zorsecyber.bouncer.api.dal.TaskDAL;
import com.zorsecyber.bouncer.api.dao.Queue;
import com.zorsecyber.bouncer.api.dao.Session;
import com.zorsecyber.bouncer.api.dao.StorageAccount;
import com.zorsecyber.bouncer.api.dao.Task;
import com.zorsecyber.bouncer.api.dao.User;
import com.zorsecyber.bouncer.api.exceptions.ErrorMessages;
import com.zorsecyber.bouncer.api.lib.SessionUtils;
import com.zorsecyber.bouncer.api.lib.msgraph.GraphFunction;
import com.zorsecyber.bouncer.api.lib.msgraph.MSGraph5;

public class GraphSubmit extends GraphFunction {
	SimpleDateFormat isoSdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

	@SuppressWarnings("null")
	@FunctionName("GraphSubmit")
	public HttpResponseMessage run(@HttpTrigger(name = "req", methods = {
			HttpMethod.POST }, authLevel = AuthorizationLevel.ANONYMOUS, route = "website/views/graph/submit") HttpRequestMessage<Optional<String>> request,
			final ExecutionContext context) throws Exception {
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

		User user;
		long userId;
		String name = null;
		Boolean obfuscateFilenames = false;
		Boolean sanitize = false;
		JSONObject returnMessage = new JSONObject();
		JSONArray successful = new JSONArray();
		JSONArray failed = new JSONArray();
		final Date until = isoSdf.parse(request.getQueryParameters().get("until"));
		final int days = Integer.parseInt(request.getQueryParameters().get("days"));
		context.getLogger().info("body: "+request.getBody().get()+" parsed "+URLDecoder.decode(request.getBody().get(), StandardCharsets.UTF_8));
		final Map<String, String> body = decodeBody(request.getBody().get());
		final ArrayList<String> mailboxes = new ArrayList<String>(
				Arrays.asList(body.get("mailboxes").split(",")));

		user = session.getUser();
		userId = user.getUserId();
		// initialize with dummy value
		long batchId = -1;
		if (request.getQueryParameters().containsKey("obfuscate")) {
			obfuscateFilenames = Boolean.parseBoolean(request.getQueryParameters().get("obfuscate"));
		}
		if (request.getQueryParameters().containsKey("sanitize")) {
			sanitize = Boolean.parseBoolean(request.getQueryParameters().get("sanitize"));
		}
		if(request.getQueryParameters().containsKey("name")) {
			name = request.getQueryParameters().get("name");
		}

		MSGraph5 graph = new MSGraph5(user.getAccessToken().getAccessToken());
		// remove unmatched mailboxes and fail them
		for (int i = mailboxes.size() - 1; i >= 0; i--) {
			String mailbox = mailboxes.get(i);
			if (!graph.canGraphUserAccessMailbox(mailbox)) {
				context.getLogger().info("invalid mailbox "+mailbox);
				failed.put(mailbox);
				mailboxes.remove(i);
			}
		}
		
		for(String s:mailboxes) {
			System.out.println(s);
		}
		
		if(!mailboxes.isEmpty()) {
			try {
				batchId = BatchDAL.createBatch(userId, name).getBatchId();
			} catch (Exception e) {
				e.printStackTrace();
				return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
						.body(ErrorMessages.wrapErrorMessage(ErrorMessages.BATCH_ERROR)).build();
			}
			Task task;
			QueueClient processingQueue = QueueDAL.createQueue(Queue.graphQueueName, StorageAccount.dataSaConnectionString);
			for (String mailbox : mailboxes) {
				try {
					task = TaskDAL.createTask(batchId, mailbox, sanitize);
				} catch (Exception ex) {
					context.getLogger().warning("Could not create task for mailbox " + mailbox + " under batch " + batchId
							+ ": " + ex.getMessage());
					continue;
				}
				String message = userId + "!" + task.getTaskId() + "!" + until.getTime() + "!" + days + "!"
						+ obfuscateFilenames.toString();
				successful.put(mailbox);
				processingQueue.sendMessage(Base64.getEncoder().encodeToString(message.getBytes()));
			}
		}
		returnMessage.put("submitted", successful);
		if (batchId != -1) {
			returnMessage.put("batch", batchId);
		}
		returnMessage.put("failed", failed);
//		 context.getLogger().info("sent messages:\n"+messages.toString());
		return request.createResponseBuilder(HttpStatus.OK).body(new JSONObject().put("data", returnMessage).toString())
				.build();

	}

}
