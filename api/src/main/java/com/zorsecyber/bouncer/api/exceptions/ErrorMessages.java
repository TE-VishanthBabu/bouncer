package com.zorsecyber.bouncer.api.exceptions;

import org.json.JSONObject;

public class ErrorMessages {

	public static final String AUTHENTICATION_FAILED = "Authentication failed";
	public static final String INVALID_DATATYPE = "Invalid datatype";
	public static final String INVALID_DIR = "Dir does not exist";
	public static final String USER_NOT_FOUND = "User not found";
	public static final String BATCH_ERROR = "Batch error";
	public static final String NO_USER_OR_SECRET = "Request body must include 'user' and 'secret'";
	public static final String REPORT_GENERATION_FAILED = "Report generation failed";
	public static final String STORAGE_ACCOUNT_ERROR = "Storage account error";
	
	public static JSONObject wrapErrorMessage(String message)
	{
		return new JSONObject().put("data", new JSONObject().put("error", message));
	}
	
}
