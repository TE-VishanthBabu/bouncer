package com.zorsecyber.bouncer.core.dependencies;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.controlcyberrisk.siaft.dependencies.HttpUtils;
import com.zorsecyber.bouncer.core.exceptions.ApiAnalysisException;
import com.zorsecyber.bouncer.core.exceptions.ApiAnalysisReportException;

/**
 * This class represents an instance of the Sophos Intelix api.
 */
public class SophosIntelixAPI implements AnalysisApi {
	public static String STATUS_SUCCESS = "SUCCESS";
	public static String STATUS_ERROR = "ERROR";
	private static final Logger log = LoggerFactory.getLogger(SophosIntelixAPI.class);
	public static final String baseURL = "https://analysis.sophos.com/";
	public static final String submitURL = "v2/files";
	public static final String reportsURL = "v2/reports";
	public static final String limitsURL = "v2/limits";
	public static final String JOB_UUID = "siaft.Intellix.job_uuid";
	public static final String SHA_256 = "siaft.Intellix.sha256";
	private String accessKey;
	private String secretKey;
	private String defaultReportLevel = "2";

	/**
	 * Constructor requires api credentials
	 * 
	 * @param accessKey SI api access key
	 * @param secretKey SI api secret key
	 */
	public SophosIntelixAPI(String accessKey, String secretKey) {
		this.accessKey = accessKey;
		this.secretKey = secretKey;
	}

	/**
	 * Submits a file to the SI api for analysis
	 * 
	 * @param filePath     Path of the file to submit
	 * @param analysisType Type of analysis (static/dynamic)
	 * @return JSONObject The http response of the file submit request in json
	 *         format
	 * @throws ApiAnalysisException
	 * @throws ClientProtocolException
	 * @throws IOException             When the file cannot be found
	 */
	public JSONObject submitFile(String filePath, String analysisType) throws ApiAnalysisException {
		String url = SophosIntelixAPI.baseURL + SophosIntelixAPI.submitURL;

		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
			HttpPost httpPost = new HttpPost(url);
			// add api auth header
			httpPost.setHeader("Authorization", "SBX2 AccessKey=" + this.accessKey + ",SecretKey=" + this.secretKey);
			// POST request data
			File file = new File(filePath);
			HttpEntity postEntities = MultipartEntityBuilder.create().addPart("file", new FileBody(file))
					.addPart("analysis_type", new StringBody(analysisType, ContentType.TEXT_PLAIN)).build();
			httpPost.setEntity(postEntities);
			String responseBody;
			try (CloseableHttpResponse response = httpclient.execute(httpPost)) {
				HttpEntity responseEntity = response.getEntity();
				responseBody = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);
				HttpUtils.consumeEntity(responseEntity);
			}
			JSONObject jsonResponse = new JSONObject(responseBody);
			return jsonResponse;
		} catch (IOException e) {
			throw new ApiAnalysisException(e);
		}
	}

	/**
	 * Retrieves an analysis report from the SI api in JSON format. Report is looked
	 * up via job uuid OR sha256
	 * 
	 * @param job_uuid     Job uuid of the analysis
	 * @param sha256       Sha256 checksum of the analyzed file
	 * @param analysisType Type of analysis that was performed (static/dynamic)
	 * @return JSONObject The analysis report in JSON format
	 * @throws JSONException           If the http response cannot be parsed as JSON
	 * @throws URISyntaxException
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public JSONObject getReport(String job_uuid, String sha256, String analysisType) throws ApiAnalysisReportException {
		String url = SophosIntelixAPI.baseURL + SophosIntelixAPI.reportsURL;
		try {
			URIBuilder params = new URIBuilder(url);
			if (sha256.equals("")) {
				params.setParameter("job_uuid", job_uuid);
			} else {
				params.setParameter("sha256", sha256);
				params.setParameter("analysis_type", analysisType);
			}
			params.setParameter("report_level", this.defaultReportLevel);
			try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
				HttpGet httpGet = new HttpGet(params.build());
				// add auth header to request
				httpGet.setHeader("Authorization", "SBX2 AccessKey=" + this.accessKey + ",SecretKey=" + this.secretKey);

				HttpEntity responseEntity;
				String responseBody;
				try (CloseableHttpResponse response = httpclient.execute(httpGet)) {
					responseEntity = response.getEntity();
					responseBody = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);
				}
				// this.log.debug("Retrieved report : " + responseBody);

				JSONObject jsonResponse = new JSONObject(responseBody);
				HttpUtils.consumeEntity(responseEntity);
				return jsonResponse;
			}
		} catch (IOException | URISyntaxException e) {
			throw new ApiAnalysisReportException(e);
		}
	}

	/**
	 * Asks the SI api how many submits are left for a certain request type in order
	 * to stay within the rate limit
	 * 
	 * @param dest Array of strings which tells the function where to look inside
	 *             the returned JSON array. Consult the SI api docs for more info
	 * @return int The number of submissions left for this type of request
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public int getSubmitsLeft(String[] dest) throws ApiAnalysisException {
		String url = SophosIntelixAPI.baseURL + SophosIntelixAPI.limitsURL;
		int available = 0;

		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
			HttpGet httpGet = new HttpGet(url);
			// add auth header to request
			httpGet.setHeader("Authorization", "SBX2 AccessKey=" + this.accessKey + ",SecretKey=" + this.secretKey);
			try (CloseableHttpResponse response = httpclient.execute(httpGet)) {
				HttpEntity responseEntity = response.getEntity();
				String responseBody = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);

				JSONObject jsonResponse = new JSONObject(responseBody);
				HttpUtils.consumeEntity(responseEntity);
				for (String k : dest) {
					jsonResponse = jsonResponse.getJSONObject(k);
				}
				available = jsonResponse.getInt("submits_left");
			}
			return available;
		} catch (IOException e) {
			throw new ApiAnalysisReportException(e);
		}
	}
}
