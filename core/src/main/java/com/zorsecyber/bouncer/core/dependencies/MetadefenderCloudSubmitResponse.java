package com.zorsecyber.bouncer.core.dependencies;

import org.json.JSONArray;
import org.json.JSONObject;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class MetadefenderCloudSubmitResponse {
	private String dataId;
	private long errorCode;
	private JSONArray errorMessages;

	public MetadefenderCloudSubmitResponse(JSONObject response) {
		dataId = null;
		errorCode = -1;
		errorMessages = null;
		if(response.has("data_id")) {
			dataId = response.getString("data_id");
		} else if(response.has("error")) {
			JSONObject error = response.getJSONObject("error");
			errorCode = error.getLong("code");
			errorMessages = error.getJSONArray("messages");
		}
	}
	
	public boolean hasError() {
		return errorCode != -1;
	}
	
}
