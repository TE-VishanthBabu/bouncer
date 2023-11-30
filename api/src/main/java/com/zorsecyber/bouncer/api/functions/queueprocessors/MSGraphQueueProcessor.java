package com.zorsecyber.bouncer.api.functions.queueprocessors;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.QueueTrigger;
import com.microsoft.graph.requests.GraphServiceClient;
import com.zorsecyber.bouncer.api.dal.TaskDAL;
import com.zorsecyber.bouncer.api.dal.UserDAL;
import com.zorsecyber.bouncer.api.dal.oauth2.MSoAuth2TokenRefresher;
import com.zorsecyber.bouncer.api.dal.oauth2.OAuth2TokenRefresher;
import com.zorsecyber.bouncer.api.dao.Task;
import com.zorsecyber.bouncer.api.dao.User;
import com.zorsecyber.bouncer.api.dao.oauth2.AccessToken;
import com.zorsecyber.bouncer.api.exceptions.OAuth2Exception;
import com.zorsecyber.bouncer.api.exceptions.QueueProcessorException;
import com.zorsecyber.bouncer.api.lib.WorkerUtils;
import com.zorsecyber.bouncer.api.lib.msgraph.MSGraph5;
import com.zorsecyber.bouncer.api.lib.pipelines.PipelineUtils;
import com.zorsecyber.bouncer.api.lib.status.TaskStatus;

import okhttp3.Request;

public class MSGraphQueueProcessor extends QueueProcessor {

	@FunctionName("MSGraphQueueProcessor")
	public void run(
			@QueueTrigger(name = "message", queueName = "%APPSETTING_bouncer.queue.graph%", connection = "APPSETTING_bouncer.storageaccount.connection-string") String message,
			final ExecutionContext context) throws Exception {
		context.getLogger().info("Processing queue message: " + message);

		AccessToken accessToken;
		String[] messageArr = message.split("!");
		long userId = Long.parseLong(messageArr[0]);
		long taskId = Long.parseLong(messageArr[1]);
		final Date until = new Date(Long.parseLong(messageArr[2]));
		int days = Integer.parseInt(messageArr[3]);
		boolean obfuscateFilenames = Boolean.parseBoolean(messageArr[4]);
		String mailbox;
		String errMsg;
		User user;
		
		user = UserDAL.getUser(userId);
		if (user == null) {
			context.getLogger().warning("Could not find user " + userId);
			throw new QueueProcessorException("Could not find user " + userId);
		}
		// update task status
		Task task;
		try {
			task = TaskDAL.getTask(taskId);
			mailbox = task.getSource();
			TaskDAL.setTaskStatus(taskId, TaskStatus.UPLOADING);
		} catch (Exception ex) {
			ex.printStackTrace();
			context.getLogger().warning("Could not get task for task" + taskId + ": " + ex.getMessage());
			TaskDAL.setTaskStatus(taskId, TaskStatus.FAILED);
			throw new QueueProcessorException("Could not get task for task" + taskId + ": " + ex.getMessage());
		}
		
		accessToken = user.getAccessToken();
		final OAuth2TokenRefresher tokenRefresher;
		try {
			tokenRefresher = new MSoAuth2TokenRefresher();
			if(tokenRefresher.tokenWillExpire(accessToken)) {
				context.getLogger().info("Refreshing tokens");
				accessToken = tokenRefresher.RefreshAccessToken(accessToken);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			TaskDAL.setTaskStatus(taskId, TaskStatus.FAILED);
			throw new OAuth2Exception("Unable to refresh tokens", ex);
		}
		
		context.getLogger().info("getting graph client");
		final GraphServiceClient<Request> graphServiceClient = new MSGraph5(accessToken.getAccessToken())
				.getGraphServiceClient();
		context.getLogger().info("got graph client");
		// assemble metadata for pipeline selection
		Map<String, Object> metadata = new HashMap<String, Object>();
		metadata.put("accessToken", accessToken);
		metadata.put("obfuscateFilenames", obfuscateFilenames);
		metadata.put("mailbox", mailbox);
		metadata.put("until", until);
		metadata.put("days", days);
		metadata.put("graphServiceClient", graphServiceClient);
		metadata.put("taskId", taskId);

		Map<String, Object> data;
		File systemTempDir = new File(System.getProperty("java.io.tmpdir"));
		File dummyFile = new File(systemTempDir, WorkerUtils.getNewWorker().toString());
		try {
			data = PipelineUtils.getAndRunPipeline(dummyFile, metadata);
			updateTaskWithResults(task, data, mailbox);
		} catch (QueueProcessorException ex) {
			errMsg = this.getClass().getSimpleName()+" failed for " + mailbox + ": " + ex.getMessage();
			TaskDAL.setTaskStatus(task.getTaskId(), TaskStatus.FAILED);
			TaskDAL.setMessage(taskId, ex.getMessage());
			context.getLogger().warning(errMsg);
			ex.printStackTrace();
			throw new QueueProcessorException(errMsg, ex);
		} finally {
			safeDeleteFile(dummyFile);
		}
	}

}
