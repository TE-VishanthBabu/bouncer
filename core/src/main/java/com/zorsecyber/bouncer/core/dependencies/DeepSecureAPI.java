package com.zorsecyber.bouncer.core.dependencies;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.tika.Tika;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.controlcyberrisk.siaft.dependencies.HttpUtils;
import com.nteligen.hq.dhs.siaft.exceptions.MimeTypeDetectionException;

/**
 * This class represents an instance of the Sophos Intelix api. 
 */
public class DeepSecureAPI implements AnalysisApi {
	/**
     * The default buffer size
     */
    public static final int DEFAULT_BUFFER_SIZE = 8192;
    
	private static final Logger log = LoggerFactory.getLogger(DeepSecureAPI.class);
	public static final String baseURL = "https://us-west-2.aws.instant.threat-removal.deep-secure.com/v1/";
	public static final String uploadURL = "upload";
	public static final String sanitizedFilesSubDir = "out/sanitized/1002/";
	public static final String failureReportsSubDir = "out/Sanitization Failed/1002/";
	private String apiKey;
	private String dataDir;

	/**
	 * Constructor requires api credentials
	 * @param accessKey	SI api access key
	 * @param secretKey	SI api secret key
	 */
	public DeepSecureAPI(String apiKey, String dataDir) {
		this.apiKey = apiKey;
		this.dataDir = HttpUtils.sanitize_path(dataDir);
	}

	/**
	 * Submits a file to the SI api for analysis
	 * @param filePath	Path of the file to submit
	 * @param analysisType	Type of analysis (static/dynamic)
	 * @return JSONObject	The http response of the file submit request
	 * in json format
	 * @throws ClientProtocolException
	 * @throws IOException	When the file cannot be found
	 */
	public JSONObject submitFile(File file) throws ClientProtocolException, IOException, MimeTypeDetectionException {
		JSONObject outcome = new JSONObject();
		Tika tika = new Tika();
		String mimeType = tika.detect(file);
		if (!MimeTypeUtils.DeepSecureAllowedMimeTypes.contains(mimeType))
		{
			throw new MimeTypeDetectionException("Mime Type "+mimeType+" not suppported by Deep Secure");
		}
		
		// create post request
		String url = DeepSecureAPI.baseURL + DeepSecureAPI.uploadURL;
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpPost httpPost = new HttpPost(url);
		
		// set request headers
		httpPost.setHeader("x-api-key", this.apiKey); // set auth header
		httpPost.setHeader("Accept", "application/json,*/*,"+mimeType); // set Accept header
		httpPost.setHeader("Content-Type", mimeType); // set Content-Type header
		httpPost.setHeader("X-Accept-Preview-Mode-For-Content-Types", mimeType);
		log.debug("Original mimeType "+mimeType);
		
		// POST request data
		HttpEntity postEntities = MultipartEntityBuilder.create()
				.addPart("path", new FileBody(file))
				.build();
		httpPost.setEntity(postEntities);
		log.debug("HttpPost :"+httpPost.toString());
		CloseableHttpResponse response = httpclient.execute(httpPost);
		HttpEntity responseEntity = response.getEntity();
		InputStream sanitizedFile = responseEntity.getContent();
		byte[] outputData = IOUtils.toByteArray(sanitizedFile);
		sanitizedFile.close();
		mimeType = tika.detect(outputData);
		
		System.out.print("Output length : "+outputData.length);
		System.out.println("Output mime type : "+mimeType);
		
		if (mimeType.equals("text/plain"))
		{
		String responseBody = new String(outputData, StandardCharsets.UTF_8);
		JSONObject jsonResponse = new JSONObject(responseBody);
		HttpUtils.consumeEntity(responseEntity);
		DeepSecureReport report = new DeepSecureReport(jsonResponse, file.getName());
		report.saveReportToFile(dataDir + failureReportsSubDir);
		return outcome.put("success", false);
		}
		else {
			File outputFile = new File(dataDir + sanitizedFilesSubDir + file.getName());
			copyByteArrayToFile(outputData, outputFile);
			return outcome.put("success", true)
					.put("sanitizedFilePath", outputFile.getAbsolutePath());
		}
	}

	/**
	 * Retrieves an analysis report from the SI api in JSON format. Report
	 * is looked up via job uuid OR sha256
	 * @param job_uuid	Job uuid of the analysis
	 * @param sha256	Sha256 checksum of the analyzed file
	 * @param analysisType	Type of analysis that was performed (static/dynamic)
	 * @return JSONObject	The analysis report in JSON format
	 * @throws JSONException	If the http response cannot be parsed as JSON
	 * @throws URISyntaxException
	 * @throws ClientProtocolException
	 * @throws IOException	
	 */
//	public JSONObject getReport(String job_uuid, String sha256, String analysisType)
//			throws JSONException, URISyntaxException, ClientProtocolException, IOException {
//		String url = SophosIntelixAPI.baseURL + SophosIntelixAPI.reportsURL;
//		URIBuilder params = new URIBuilder(url);
//		if (sha256.equals("")) {
//			params.setParameter("job_uuid", job_uuid);
//		} else {
//			params.setParameter("sha256", sha256);
//			params.setParameter("analysis_type", analysisType);
//		}
//		params.setParameter("report_level", this.defaultReportLevel);
//		CloseableHttpClient httpclient = HttpClients.createDefault();
//		HttpGet httpGet = new HttpGet(params.build());
//		// add auth header to request
//		httpGet.setHeader("Authorization", "SBX2 AccessKey=" + this.accessKey + ",SecretKey=" + this.secretKey);
//
//		CloseableHttpResponse response = httpclient.execute(httpGet);
//		HttpEntity responseEntity = response.getEntity();
//		String responseBody = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);
//		this.log.debug("Retrieved report : " + responseBody);
//
//		JSONObject jsonResponse = new JSONObject(responseBody);
//		HttpUtils.consumeEntity(responseEntity);
//		return jsonResponse;
//	}
//
//	/**
//	 * Asks the SI api how many submits are left for a certain request type
//	 * in order to stay within the rate limit
//	 * @param dest	Array of strings which tells the function where to look inside
//	 * the returned JSON array. Consult the SI api docs for more info
//	 * @return int	The number of submissions left for this type of request
//	 * @throws ClientProtocolException
//	 * @throws IOException
//	 */
//	public int getSubmitsLeft(String[] dest) throws ClientProtocolException, IOException {
//		String url = SophosIntelixAPI.baseURL + SophosIntelixAPI.limitsURL;
//		int available = 0;
//
//		CloseableHttpClient httpclient = HttpClients.createDefault();
//		HttpGet httpGet = new HttpGet(url);
//		// add auth header to request
//		httpGet.setHeader("Authorization", "SBX2 AccessKey=" + this.accessKey + ",SecretKey=" + this.secretKey);
//		CloseableHttpResponse response = httpclient.execute(httpGet);
//		HttpEntity responseEntity = response.getEntity();
//		String responseBody = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);
//
//		JSONObject jsonResponse = new JSONObject(responseBody);
//		HttpUtils.consumeEntity(responseEntity);
//		for (String k : dest) {
//			jsonResponse = jsonResponse.getJSONObject(k);
//		}
//		available = jsonResponse.getInt("submits_left");
//		return available;
//	}
	
	private static void copyByteArrayToFile(byte[] data, File file)
            throws IOException {

        // append = false
        try (FileOutputStream outputStream = new FileOutputStream(file, false)) {
            outputStream.write(data);
        }

    }
}
