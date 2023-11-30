package com.zorsecyber.bouncer.api.functions.views;

import java.util.Optional;

import org.json.JSONArray;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.BindingName;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.zorsecyber.bouncer.api.dal.SessionDAL;
import com.zorsecyber.bouncer.api.dao.Session;
import com.zorsecyber.bouncer.api.dao.User;
import com.zorsecyber.bouncer.api.exceptions.ErrorMessages;
import com.zorsecyber.bouncer.api.functions.submit.Submit;
import com.zorsecyber.bouncer.api.lib.SessionUtils;
import com.zorsecyber.bouncer.api.lib.reports.PGReportV1;
import com.zorsecyber.bouncer.api.lib.storage.AzureBlobContainerClient;
import com.zorsecyber.bouncer.api.lib.storage.AzureFileShareClient;
import com.zorsecyber.bouncer.api.lib.storage.AzureStorageClient;

public class FilesView extends WebsiteView {

	@FunctionName("FilesView")
	public HttpResponseMessage run(@HttpTrigger(name = "req", methods = {
			HttpMethod.GET }, authLevel = AuthorizationLevel.ANONYMOUS, route = "website/views/{dataSource}/filesView") HttpRequestMessage<Optional<String>> request,
			@BindingName("dataSource") String dataSource, final ExecutionContext context) {
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
		context.getLogger().info("Got user: " + user.getUserId());

		// validate datasource
		if (!Submit.allowedDataSourceTypes.contains(dataSource)) {
			return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
					.body(ErrorMessages.wrapErrorMessage(ErrorMessages.INVALID_DATATYPE)).build();
		}

		// get list of files
		context.getLogger().info("listing files for userId " + user.getUserId() + " from dataSource : " + dataSource);
		JSONArray uploadedFiles = new JSONArray();
		// retrieve list of files from target data source type
		String dir = "";
		AzureStorageClient storageClient = null;
		if (dataSource.equals("fileshare")) {
			storageClient = new AzureFileShareClient(user);
		} else if (dataSource.equals("blobcontainer")) {
			storageClient = new AzureBlobContainerClient(user);
		}
		if (storageClient == null) {
			return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ErrorMessages.wrapErrorMessage(ErrorMessages.STORAGE_ACCOUNT_ERROR)).build();
		}
		uploadedFiles = storageClient.listFilesAndDirectories();
		return request.createResponseBuilder(HttpStatus.OK)
				.body(PGReportV1.buildAvailableFilesView(uploadedFiles).toString()).build();
	}
}
