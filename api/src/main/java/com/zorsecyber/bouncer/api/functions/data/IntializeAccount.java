package com.zorsecyber.bouncer.api.functions.data;

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
import com.zorsecyber.bouncer.api.dal.SessionDAL;
import com.zorsecyber.bouncer.api.dal.UserDAL;
import com.zorsecyber.bouncer.api.dao.Session;
import com.zorsecyber.bouncer.api.dao.User;
import com.zorsecyber.bouncer.api.exceptions.ErrorMessages;
import com.zorsecyber.bouncer.api.functions.views.WebsiteView;
import com.zorsecyber.bouncer.api.lib.SessionUtils;
import com.zorsecyber.bouncer.api.lib.storage.AzureFileShareClient;

public class IntializeAccount extends WebsiteView {

	@FunctionName("InitializeAccount")
	public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET},
                authLevel = AuthorizationLevel.ANONYMOUS,
                		route = "website/views/user/{userId}/init")
                HttpRequestMessage<Optional<String>> request,
                @BindingName("userId") String userId,
            final ExecutionContext context)
	{
		User user;
		try {
			user = UserDAL.getUser(Long.parseLong(userId));
			context.getLogger().info("Got user: "+user.getUserId());
			
			boolean initSuccess = false;
			initSuccess = AzureFileShareClient.createUserDataDir(user);
			JSONObject data = new JSONObject()
					.put("data", new JSONObject()
							.put("success", initSuccess));
			return request.createResponseBuilder(HttpStatus.OK).body(data.toString()).build();
		} catch (Exception e) {
			e.printStackTrace();
			return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed: "+e.getMessage()).build();
		}
	}

}
