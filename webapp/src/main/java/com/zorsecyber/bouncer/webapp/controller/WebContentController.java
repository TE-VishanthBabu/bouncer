package com.zorsecyber.bouncer.webapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.zorsecyber.bouncer.webapp.WebPageController;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@CrossOrigin
public class WebContentController extends WebPageController {

	@GetMapping("/")
	public String landing(Model model) {
		return "redirect:/dashboard";
	}

	@GetMapping("/account")
	public String account(Model model, HttpServletRequest request) {
		model.addAllAttributes(loadPageAttributes(request));
		model.addAttribute("templateName", addBouncerSuffix("Account"));
		return "account";
	}

	@GetMapping("/dashboard")
	public String dashboard(Model model, HttpServletRequest request) {
		model.addAllAttributes(loadPageAttributes(request));
		model.addAttribute("templateName", addBouncerSuffix("Dashboard"));
		return "dashboard";
	}

	@GetMapping("{batchId}/report")
	public String tables(@PathVariable("batchId") Long batchId, @RequestParam String name, Model model,
			HttpServletRequest request) {
		model.addAllAttributes(loadPageAttributes(request));
		model.addAttribute("batchId", batchId);
		model.addAttribute("name", name);
		model.addAttribute("templateName", addBouncerSuffix("Report (" + name + ")"));
		return "report";
	}

	@GetMapping("{fileAttributeId}/{sha256}/fileDetails")
	public String fileDetails(@PathVariable("sha256") String sha256,
			@PathVariable("fileAttributeId") long fileAttributeId, Model model, HttpServletRequest request) {
		model.addAllAttributes(loadPageAttributes(request));
		model.addAttribute("templateName", addBouncerSuffix(sha256));
		model.addAttribute(fileAttributeId);
		return "fileDetails";
	}

	@GetMapping("/analyze/files")
	public String analyzeFiles(Model model, HttpServletRequest request) {
		model.addAllAttributes(loadPageAttributes(request));
		model.addAttribute("templateName", addBouncerSuffix("Analyze Files"));
		return "analyzeFiles";
	}

	@GetMapping("/analyze/mailboxes")
	public String analyzeMailboxes(Model model, HttpServletRequest request) {
		model.addAllAttributes(loadPageAttributes(request));
		model.addAttribute("templateName", addBouncerSuffix("Analyze Mailboxes"));
		return "analyzeMailboxes";
	}

	@GetMapping("/unlicensed/landing")
	public String unlicensedLandingPage() {
		return "unlicensedLanding";
	}
	
	@GetMapping("/admin/home")
	public String adminHomePage() {
		return "adminHome";
	}

}
