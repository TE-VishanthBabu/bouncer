package com.zorsecyber.bouncer.api.functions.views;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;

public class WebsiteView {
	protected int offset = 0;
	protected int length = 0;
	protected int orderDirection = -1;
	protected String orderColumn;
	protected String q = null;
	protected String qColumn;
	
	public String getJwt(HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context)
	{
		// get jwt from header
		String jwt = "";
		if(request.getHeaders().containsKey("jwt"))
		{
		jwt = request.getHeaders().get("jwt");
		}
		else 
		{
			context.getLogger().info("Jwt header not present");
			return null;
		}
		return jwt;
	}
	
	protected Boolean getLengthAndOffset(HttpRequestMessage<Optional<String>> request)
	{
		if(request.getQueryParameters().containsKey("offset") && request.getQueryParameters().containsKey("length"))
		{
			offset = Integer.parseInt(request.getQueryParameters().get("offset"));
			length = Integer.parseInt(request.getQueryParameters().get("length"));
		} else {
			return false;
		}
		return true;
	}
	
	protected void getOrderDirection(HttpRequestMessage<Optional<String>> request)
	{
		if(request.getQueryParameters().containsKey("orderDirection"))
		{
			orderDirection = Integer.parseInt(request.getQueryParameters().get("orderDirection"));
		}
	}
	
	protected void getOrderColumn(HttpRequestMessage<Optional<String>> request)
	{
		if(request.getQueryParameters().containsKey("orderColumn"))
			orderColumn = request.getQueryParameters().get("orderColumn").toLowerCase();
	}
	
	protected void getSearchQuery(HttpRequestMessage<Optional<String>> request)
	{
		if(request.getQueryParameters().containsKey("q"))
			q = request.getQueryParameters().get("q");
	}
	
	protected Map<String, String> decodeBody(String body) {
		Map<String, String> bodyMap = new HashMap<String, String>();
		body = URLDecoder.decode(body, StandardCharsets.UTF_8);
		for(String data : body.split("&")) {
			String[] kvp = data.split("=");
			if(kvp.length>1)
				bodyMap.put(kvp[0], kvp[1]);
		}
		return bodyMap;
	}

}
