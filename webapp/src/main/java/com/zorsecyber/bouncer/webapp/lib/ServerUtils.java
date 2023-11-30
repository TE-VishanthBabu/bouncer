package com.zorsecyber.bouncer.webapp.lib;

import jakarta.servlet.http.HttpServletRequest;

public class ServerUtils {
    public static String getBaseUrl(HttpServletRequest request) {
    	if(request.getServerPort() != 80) {
    		return String.format("%s://%s:%d/",request.getScheme(),  request.getServerName(), request.getServerPort());
    	}
    	return String.format("%s://%s/",request.getScheme(),  request.getServerName());
    }
}
