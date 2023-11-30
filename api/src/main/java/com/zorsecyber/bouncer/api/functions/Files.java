package com.zorsecyber.bouncer.api.functions;

import java.util.Optional;

import org.json.JSONArray;
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
import com.zorsecyber.bouncer.api.dal.UserDAL;
import com.zorsecyber.bouncer.api.dao.User;
import com.zorsecyber.bouncer.api.exceptions.ErrorMessages;
import com.zorsecyber.bouncer.api.functions.submit.Submit;
import com.zorsecyber.bouncer.api.lib.AuthUtils;
import com.zorsecyber.bouncer.api.lib.storage.AzureBlobContainerClient;
import com.zorsecyber.bouncer.api.lib.storage.AzureFileShareClient;
import com.zorsecyber.bouncer.api.lib.storage.AzureStorageClient;

/**
 * 
 * @author GEruo This function lists the pst files available to the user to
 *         submit for analysis
 * @binding dataSource The datasource to list files from (fileshare or
 *          blobcontainer)
 * @param user   The user id of the requesting user
 * @param secret The secret of the requesting user
 * @response 200 The request completed successfully
 * @response 400 Invalid datasource or user not found
 * @response 401 The user/secret authorization failed
 */
public class Files {
	private static String defaultDir = "";

	@FunctionName("Files")
	public HttpResponseMessage run(@HttpTrigger(name = "req", methods = {
			HttpMethod.GET }, authLevel = AuthorizationLevel.ANONYMOUS, route = "{datatype}/files") HttpRequestMessage<Optional<String>> request,
			@BindingName("datatype") String dataSource, final ExecutionContext context)
			throws NumberFormatException, Exception {

		// validate datasource
		if (!Submit.allowedDataSourceTypes.contains(dataSource)) {
			return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
					.body(ErrorMessages.wrapErrorMessage(ErrorMessages.INVALID_DATATYPE)).build();
		}
		// verify required parameters are set
		if (!request.getQueryParameters().containsKey("user") || !request.getQueryParameters().containsKey("secret")) {
			return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body(ErrorMessages.wrapErrorMessage(ErrorMessages.NO_USER_OR_SECRET)).build();
		}

		// set variables
		final long userId = Long.parseLong(request.getQueryParameters().get("user"));
		final String secret = request.getQueryParameters().get("secret");
		final String dir;
		User user = null;

		if (request.getQueryParameters().containsKey("dir")) {
			dir = request.getQueryParameters().get("dir");
		} else {
			dir = defaultDir;
		}

		// authorize user
		try {
			// lookup the requesting user
			user = UserDAL.getUser(userId);
			if (!AuthUtils.authCredentials(userId, secret)) {
				return request.createResponseBuilder(HttpStatus.UNAUTHORIZED)
						.body(ErrorMessages.wrapErrorMessage(ErrorMessages.AUTHENTICATION_FAILED)).build();
			}
		} catch (Exception e) {
			return request.createResponseBuilder(HttpStatus.NOT_FOUND)
					.body(ErrorMessages.wrapErrorMessage(ErrorMessages.USER_NOT_FOUND)).build();
		}

		// get list of files
		context.getLogger().info("listing files for userId " + userId + " from dataSource : " + dataSource);
		JSONArray uploadedFiles = new JSONArray();
		// retrieve list of files from target data source type
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
		uploadedFiles = storageClient.listFilesAndDirectories();
		return request.createResponseBuilder(HttpStatus.OK).body(new JSONObject().put("data", uploadedFiles).toString())
				.build();
	}
}
