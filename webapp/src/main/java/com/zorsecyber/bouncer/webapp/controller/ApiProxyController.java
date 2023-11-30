package com.zorsecyber.bouncer.webapp.controller;

import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.zorsecyber.bouncer.webapp.constant.Constant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/proxy")
public class ApiProxyController {
	
	@Value("${storage.userdata.source}")
	public String userDataSource;

	@Value("${api.url}")
	public String apiBaseURL;
	
	@Value("${api.key}")
	public String apiKey;
	
	public static final String batchViewURL = "batchView";
	public static final String taskViewURL = "/taskView";
	public static final String fileSubmissionsViewURL = "/filesubmissionsView";
	public static final String fileDetailsViewURL = "/filedetailsView";
	public static final String indicatorViewURL = "/indicatorView";
	public static final String submitViewURL = "/submitView";
	public static final String filesViewURL = "/filesView";
	public static final String mailboxesViewURL = "graph/mailboxes";
	public static final String graphSubmitViewURL = "graph/submit";
	public static final String filetypeSummaryURL = "/filetypeSummaryView";
	public static final String deleteURL = "/delete";
	
	public String getRequest(String url, String jwt, List<NameValuePair> params) throws URISyntaxException
	{
		URIBuilder uriParams = new URIBuilder(url);
		if(params != null)
		{
			uriParams.addParameters(params);
		}
		
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpGet httpGet = new HttpGet(uriParams.build());
		// add auth headers to request
		httpGet.setHeader("Content-Type", "application/json");
		httpGet.setHeader("Ocp-Apim-Subscription-Key", apiKey);
		httpGet.setHeader("jwt", jwt);

		String responseBody = null;
		try
		{
		CloseableHttpResponse proxyResponse = httpclient.execute(httpGet);
		HttpEntity responseEntity = proxyResponse.getEntity();
		responseBody = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);

		JSONObject jsonResponse = new JSONObject(responseBody);
		return jsonResponse.toString();
		}
		catch(Exception e)
		{
			JSONObject error = new JSONObject()
					.put("error", 
							new JSONObject()
							.put("message", e.getMessage())
							.put("responsebody", responseBody));
			return error.toString();
		}
	}
	
	public String postRequest(String url, String jwt, List<NameValuePair> params, String body) throws URISyntaxException
	{
		URIBuilder uriParams = new URIBuilder(url);
		if(params != null)
		{
			uriParams.addParameters(params);
		}

		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpPost httpPost = new HttpPost(uriParams.build());
		// add auth headers to request
		httpPost.setHeader("Ocp-Apim-Subscription-Key", apiKey);
		httpPost.setHeader("jwt", jwt);
		
		// post request body
		if(body != null) {
			httpPost.setEntity(new StringEntity(body, StandardCharsets.UTF_8));
		}

		String responseBody = null;
		try
		{
		CloseableHttpResponse proxyResponse = httpclient.execute(httpPost);
		HttpEntity responseEntity = proxyResponse.getEntity();
		responseBody = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);

		JSONObject jsonResponse = new JSONObject(responseBody);
		return jsonResponse.toString();
		}
		catch(Exception e)
		{
			JSONObject error = new JSONObject()
					.put("error",
							new JSONObject()
							.put("message", e.getMessage())
							.put("responsebody", responseBody));
			return error.toString();
		}
	}
	
	public String deleteRequest(String url, String jwt) throws URISyntaxException
	{
		URIBuilder uriParams = new URIBuilder(url);

		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpDelete httpDelete = new HttpDelete(uriParams.build());
		// add auth headers to request
		httpDelete.setHeader("Ocp-Apim-Subscription-Key", apiKey);
		httpDelete.setHeader("jwt", jwt);

		String responseBody = null;
		try
		{
		CloseableHttpResponse proxyResponse = httpclient.execute(httpDelete);
		HttpEntity responseEntity = proxyResponse.getEntity();
		responseBody = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);

		JSONObject jsonResponse = new JSONObject(responseBody);
		return jsonResponse.toString();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			log.warn("exception: "+e.getMessage());
			JSONObject error = new JSONObject().put("error", e.getMessage()).put("body", responseBody);
			return new JSONObject().put("data", error).toString();
		}
	}
	
	public String putRequest(String url, String jwt, List<NameValuePair> params) throws URISyntaxException
	{
		URIBuilder uriParams = new URIBuilder(url);
		if(params != null)
		{
			uriParams.addParameters(params);
		}

		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpPut httpPut = new HttpPut(uriParams.build());
		// add auth headers to request
		httpPut.setHeader("Ocp-Apim-Subscription-Key", apiKey);
		httpPut.setHeader("jwt", jwt);

		String responseBody = null;
		try
		{
		CloseableHttpResponse proxyResponse = httpclient.execute(httpPut);
		HttpEntity responseEntity = proxyResponse.getEntity();
		responseBody = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);

		JSONObject jsonResponse = new JSONObject(responseBody);
		return jsonResponse.toString();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			log.warn("exception: "+e.getMessage());
			JSONObject error = new JSONObject().put("error", e.getMessage()).put("body", responseBody);
			return new JSONObject().put("data", error).toString();
		}
	}
	
	@RequestMapping("batchview")
	@ResponseBody
	public String batchView(@RequestParam int offset, @RequestParam int length, @RequestParam int orderDirection,
			HttpServletRequest request, HttpServletResponse response, HttpSession session) throws Exception {
		String jwt = getJwt(session);
		String url = apiBaseURL + batchViewURL;
		List<NameValuePair> params = setLengthAndOffsetParams(length, offset);
		params.add(new BasicNameValuePair("orderDirection", Integer.toString(orderDirection)));
		return getRequest(url, jwt, params);
	}
	
	@RequestMapping("{batchId}/taskview")
	@ResponseBody
	public String batchView(@RequestParam int offset, @RequestParam int length, @RequestParam int orderDirection, @RequestParam(required = false) String orderColumn,
			@PathVariable("batchId") long batchId, HttpServletRequest request, HttpServletResponse response, HttpSession session) throws Exception {
		String jwt = getJwt(session);
		String url = apiBaseURL + batchId + taskViewURL;
		List<NameValuePair> params = setLengthAndOffsetParams(length, offset);
		params.add(new BasicNameValuePair("orderDirection", Integer.toString(orderDirection)));
		params.add(new BasicNameValuePair("orderColumn", orderColumn));
		return getRequest(url, jwt, params);
	}
	
	@RequestMapping("{taskId}/filesubmissionsview")
	@ResponseBody
	public String filesubmissionsView(@RequestParam int offset, @RequestParam int length, @RequestParam int orderDirection, @RequestParam(required = false) String query,
			@RequestParam(required = false) String orderColumn, @PathVariable("taskId") long taskId, HttpServletRequest request, HttpServletResponse response, HttpSession session) throws Exception {
		String jwt = getJwt(session);
		String url = apiBaseURL + taskId + fileSubmissionsViewURL;
		List<NameValuePair> params = setLengthAndOffsetParams(length, offset);
		params.add(new BasicNameValuePair("orderDirection", Integer.toString(orderDirection)));
		params.add(new BasicNameValuePair("q", query));
		if(orderColumn!=null)
			params.add(new BasicNameValuePair("orderColumn", orderColumn));
		return getRequest(url, jwt, params);
	}
	
	@RequestMapping("{sha256}/filedetailsview")
	@ResponseBody
	public String filedetailsView(@PathVariable("sha256") String sha256,
			HttpServletRequest request, HttpServletResponse response, HttpSession session) throws Exception {
		String jwt = getJwt(session);
		String url = apiBaseURL + sha256 + fileDetailsViewURL;
		return getRequest(url, jwt, null);
	}
	
	@RequestMapping("{siAnalysisId}/indicatorview")
	@ResponseBody
	public String indicatorView(@PathVariable("siAnalysisId") long siAnalysisId,
			HttpServletRequest request, HttpServletResponse response, HttpSession session) throws Exception {
		String jwt = getJwt(session);
		String url = apiBaseURL + siAnalysisId + indicatorViewURL;
		return getRequest(url, jwt, null);
	}
	
	@RequestMapping("filesview")
	@ResponseBody
	public String filesView(HttpServletRequest request, HttpServletResponse response, HttpSession session) throws Exception {
		String jwt = getJwt(session);
		String url = apiBaseURL + userDataSource + filesViewURL;
		return getRequest(url, jwt, null);
	}
	
	@RequestMapping("graph/mailboxes")
	@ResponseBody
	public String mailboxesView(@RequestParam int offset, @RequestParam int length, 
			HttpServletRequest request, HttpServletResponse response, HttpSession session) throws URISyntaxException {
		final String requestTokenHeader = String.valueOf(session.getAttribute("Authorization"));
		String jwt = "";
		
		if (requestTokenHeader != null && requestTokenHeader.startsWith(Constant.BEARER)) {
            jwt = requestTokenHeader.substring(7);
		}
		String url = apiBaseURL + mailboxesViewURL;
		List<NameValuePair> params = setLengthAndOffsetParams(length, offset);
		return getRequest(url, jwt, params);
	}
	
	@RequestMapping(value = "submitview", method = POST)
	@ResponseBody
	public String submitView(@RequestBody String body, @RequestParam String obfuscate,
		@RequestParam String sanitize, @RequestParam(required = false) String name, HttpServletRequest request, HttpServletResponse response, HttpSession session) throws Exception {
		String jwt = getJwt(session);
		String url = apiBaseURL + userDataSource + submitViewURL;
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("obfuscate", obfuscate));
		params.add(new BasicNameValuePair("sanitize", sanitize));
		params.add(new BasicNameValuePair("name", name));
		return postRequest(url, jwt, params, body);
	}
	
	@RequestMapping("{batchId}/filetypeSummaryView")
	@ResponseBody
	public String filetypeSummaryView(@PathVariable("batchId") long batchId,
			HttpServletRequest request, HttpServletResponse response, HttpSession session) throws Exception {
		String jwt = getJwt(session);
		String url = apiBaseURL + batchId + filetypeSummaryURL;
		return getRequest(url, jwt, null);
	}
	
	@RequestMapping(value = "{entity}/{id}/delete", method = DELETE)
	@ResponseBody
	public String delete(@PathVariable("entity") String entity, @PathVariable("id") long id,
		HttpServletRequest request, HttpServletResponse response, HttpSession session) throws Exception {
		String jwt = getJwt(session);
		String url = apiBaseURL + entity + "/" + id + "/delete";
		return deleteRequest(url, jwt);
	}
	
	@RequestMapping(value = "{entity}/{id}/{field}", method = PUT)
	@ResponseBody
	public String put(@PathVariable("entity") String entity, @PathVariable("id") long id, @PathVariable("field") String field,
		@RequestParam("value") String value, HttpServletRequest request, HttpServletResponse response, HttpSession session) throws Exception {
		String jwt = getJwt(session);
		String url = apiBaseURL + entity + "/" + id + "/" + field;
		log.info(url);
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("value", value));
		return putRequest(url, jwt, params);
	}
	
	@RequestMapping("graph/graphsubmitview")
	@ResponseBody
	public String graphSubmitView(@RequestParam String until, @RequestBody String body, @RequestParam int days, @RequestParam boolean obfuscate,
			@RequestParam boolean sanitize, @RequestParam String name, HttpServletRequest request, HttpServletResponse response, HttpSession session) throws URISyntaxException {
		final String requestTokenHeader = String.valueOf(session.getAttribute("Authorization"));
		String jwt = "";
		
		if (requestTokenHeader != null && requestTokenHeader.startsWith(Constant.BEARER)) {
            jwt = requestTokenHeader.substring(7);
		}
		String url = apiBaseURL + ApiProxyController.graphSubmitViewURL;
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("days", Integer.toString(days)));
		params.add(new BasicNameValuePair("until", until));
		params.add(new BasicNameValuePair("obfuscate", Boolean.toString(obfuscate)));
		params.add(new BasicNameValuePair("sanitize", Boolean.toString(sanitize)));
		params.add(new BasicNameValuePair("name", name));
		return postRequest(url, jwt, params, body);
	}
	
	private List<NameValuePair> setLengthAndOffsetParams(int length, int offset)
	{
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("offset", Integer.toString(offset)));
		params.add(new BasicNameValuePair("length", Integer.toString(length)));
		return params;
	}
	
	protected Map<String, String> decodeBody(String body) {
		Map<String, String> bodyMap = new HashMap<String, String>();
		for(String data : body.split("&")) {
			String[] kvp = data.split("=");
			if(kvp.length>1)
				bodyMap.put(kvp[0], kvp[1]);
		}
		return bodyMap;
	}
	
	private String getJwt(HttpSession session) throws Exception {
		try {
		final String requestTokenHeader = String.valueOf(session.getAttribute("Authorization"));
		if (requestTokenHeader.startsWith(Constant.BEARER)) {
            return requestTokenHeader.substring(7);
		} else {
			throw new Exception("jwt does not begin with "+Constant.BEARER);
		}
		} catch (Exception ex) {
			throw new Exception("Could not retrieve jwt from request");
		}
	}

}
