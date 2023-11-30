package com.zorsecyber.bouncer.api.functions.submit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
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
import com.microsoft.azure.functions.annotation.BindingName;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.zorsecyber.bouncer.api.dal.BatchDAL;
import com.zorsecyber.bouncer.api.dal.QueueDAL;
import com.zorsecyber.bouncer.api.dal.SessionDAL;
import com.zorsecyber.bouncer.api.dal.TaskDAL;
import com.zorsecyber.bouncer.api.dao.Batch;
import com.zorsecyber.bouncer.api.dao.Queue;
import com.zorsecyber.bouncer.api.dao.Session;
import com.zorsecyber.bouncer.api.dao.StorageAccount;
import com.zorsecyber.bouncer.api.dao.Task;
import com.zorsecyber.bouncer.api.dao.User;
import com.zorsecyber.bouncer.api.exceptions.ErrorMessages;
import com.zorsecyber.bouncer.api.functions.views.WebsiteView;
import com.zorsecyber.bouncer.api.lib.SessionUtils;
import com.zorsecyber.bouncer.api.lib.storage.AzureBlobContainerClient;
import com.zorsecyber.bouncer.api.lib.storage.AzureFileShareClient;
import com.zorsecyber.bouncer.api.lib.storage.AzureStorageClient;

public class Submit extends WebsiteView {
	public static List<String> allowedDataSourceTypes = Arrays.asList("blobcontainer");

	@FunctionName("SubmitView")
	public HttpResponseMessage run(@HttpTrigger(name = "req", methods = {
			HttpMethod.POST }, authLevel = AuthorizationLevel.ANONYMOUS, route = "website/views/{datasource}/submitView") HttpRequestMessage<Optional<String>> request,
			@BindingName("datasource") String dataSource, final ExecutionContext context) {

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

		User user = session.getUser();

		// validate datasource
		if (!allowedDataSourceTypes.contains(dataSource)) {
			return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
					.body(ErrorMessages.wrapErrorMessage(ErrorMessages.INVALID_DATATYPE)).build();
		}

		// set variables
		final long userId = user.getUserId();
		context.getLogger().info("userid " +user.getUserId());
		final Map<String, String> body = decodeBody(request.getBody().get());
		context.getLogger().info("body " + body.toString());
		final String append = request.getQueryParameters().get("append");
		String name = null;
		Boolean obfuscateFilenames = false;
		Boolean sanitizeFiles = false;
		JSONObject returnMessage = new JSONObject();
		JSONArray submittedPsts = new JSONArray();
		JSONArray failed = new JSONArray();
		ArrayList<String> fileNames = new ArrayList<String>(Arrays.asList(body.get("files").split(",")));
		ArrayList<String> uploadedFiles = null;
		// initialize with dummy value
		long batchId = -1;
		if (request.getQueryParameters().containsKey("obfuscate")) {
			obfuscateFilenames = Boolean.parseBoolean(request.getQueryParameters().get("obfuscate"));
		}
		if (request.getQueryParameters().containsKey("sanitize")) {
			sanitizeFiles = Boolean.parseBoolean(request.getQueryParameters().get("sanitize"));
		}
		if(request.getQueryParameters().containsKey("name")) {
			name = request.getQueryParameters().get("name");
		}

		// retrieve list of available files
		AzureStorageClient storageClient = null;
		if (dataSource.equals("fileshare")) {
			storageClient = new AzureFileShareClient(user);
		} else if (dataSource.equals("blobcontainer")) {
			storageClient = new AzureBlobContainerClient(user);
		}
		if(storageClient == null) {
			return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ErrorMessages.wrapErrorMessage(ErrorMessages.STORAGE_ACCOUNT_ERROR)).build();
		}
		uploadedFiles = AzureStorageClient.jsonArrayToList1d(storageClient.listFilesAndDirectories());
		// remove unmatched pst names and fail them
		try {
			for (int i = fileNames.size() - 1; i >= 0; i--) {
				String pstName = fileNames.get(i);
				if (!uploadedFiles.contains(pstName)) {
					failed.put(pstName);
					fileNames.remove(i);
				}
			}
		} catch (NullPointerException e) {
			context.getLogger().warning("uploaded file list is null "+e.getMessage());
			return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ErrorMessages.wrapErrorMessage(ErrorMessages.STORAGE_ACCOUNT_ERROR)).build();
		}

		// check that there is at least one valid pst name
		if (!fileNames.isEmpty()) {
			if(append == null) {
				// create new batch
				try {
					batchId = BatchDAL.createBatch(userId, name).getBatchId();
					context.getLogger().info("Created batch "+batchId+" with name "+name);
				} catch (Exception e) {
					e.printStackTrace();
					return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
							.body(ErrorMessages.wrapErrorMessage(ErrorMessages.BATCH_ERROR)).build();
				}
			} else {
				try {
					Batch batch = BatchDAL.getBatch(Long.parseLong(append));
					batchId = batch.getBatchId();
					context.getLogger().info("Appending to batch "+batchId);
				} catch (Exception e) {
					e.printStackTrace();
					return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
							.body(ErrorMessages.wrapErrorMessage(ErrorMessages.BATCH_ERROR)).build();
				}
			}

			// submit valid psts
			Task task;
			context.getLogger().info("using queue: "+Queue.filesQueueName);
			QueueClient processingQueue = QueueDAL.createQueue(Queue.filesQueueName, StorageAccount.dataSaConnectionString);
			for (String pstName : fileNames) {
				try {
					task = TaskDAL.createTask(batchId, pstName, sanitizeFiles);
				} catch (Exception ex) {
					context.getLogger().warning("Could not create task for file " + pstName + " under batch " + batchId
							+ ": " + ex.getMessage());
					continue;
				}
				String message = userId + "!" + task.getTaskId() + "!" + dataSource + "!"
						+ obfuscateFilenames.toString();
				submittedPsts.put(pstName);
				processingQueue.sendMessage(Base64.getEncoder().encodeToString(message.getBytes()));
			}
		}
		returnMessage.put("submitted", submittedPsts);
		if (batchId != -1) {
			returnMessage.put("batch", batchId);
		}
		returnMessage.put("failed", failed);
		// System.out.println("sent messages:\n"+messages.toString());
		return request.createResponseBuilder(HttpStatus.OK).body(new JSONObject().put("data", returnMessage).toString())
				.build();
	}

}
