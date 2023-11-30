package com.controlcyberrisk.siaft.dependencies;

import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

public class HttpUtils {
	public static void printJSONResponseAsString(JSONObject responseJSON) {
		try {
        System.out.println(responseJSON);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void consumeEntity(HttpEntity response) {
		try {
			EntityUtils.consume(response);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static String sanitize_path(String path) {
		if (!path.substring(path.length() - 1).equals("/")) {
			// if not, append '/'
			path += "/";
		}
		return path;
	}
}
