package com.zorsecyber.bouncer.webapp;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.zorsecyber.bouncer.webapp.service.UserService;

import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;

public class WebPageController {
	@Autowired
	private UserService userService;
	
	private String dashboardUrl = "/dashboard";
	private String analyzeFilesUrl = "/analyze/files";
	private String analyzeMailboxesUrl = "/analyze/mailboxes";
	private String uploadFilesUrl = "/upload";
	private String privacyPolicyUrl = "#";
	private String tAndcUrl = "#";
	

	
	protected Map<String, String> loadPageAttributes(HttpServletRequest request) {
		String email = userService.getUserEmail(request);
		Map<String, String> model = new HashMap<String, String>();
		model.put("userEmail", email);
		
		model.put("dashboardUrl", dashboardUrl);
		model.put("analyzeFilesUrl", analyzeFilesUrl);
		model.put("analyzeMailboxesUrl", analyzeMailboxesUrl);
		model.put("uploadFilesUrl", uploadFilesUrl);
		model.put("privacyPolicyUrl", privacyPolicyUrl);
		model.put("tAndcUrl", tAndcUrl);
		
		
		return model;
	}
	
	protected String addBouncerSuffix(Object o) {
		return o.toString() + " - Bouncer";
	}
	
}
