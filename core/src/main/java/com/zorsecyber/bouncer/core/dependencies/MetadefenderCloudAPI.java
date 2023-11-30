package com.zorsecyber.bouncer.core.dependencies;

import java.io.File;
import java.io.IOException;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.controlcyberrisk.siaft.dependencies.HttpUtils;

import okhttp3.FormBody;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MetadefenderCloudAPI {
	public static final long ERROR_THROTTLED = 429001;
	private static final Logger log = LoggerFactory.getLogger(MetadefenderCloudAPI.class);
	public static final String baseUrl = "https://api.metadefender.com/v4/";
	public static final String fileUrl = "file";
	public static final String rule = "cdr";
	public String apiKey;
	private File reportRepo;

	public MetadefenderCloudAPI(String apiKey, File reportRepo)
	{
		this.apiKey = apiKey;
		this.reportRepo = reportRepo;
	}
	
	public JSONObject submitFile(File file) throws IOException
	{
		OkHttpClient client = new OkHttpClient();

		RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getAbsolutePath(), RequestBody.create(null, file))
                .build();
		Request request = new Request.Builder()
		  .url(baseUrl+fileUrl)
		  .post(requestBody)
		  .addHeader("apikey", this.apiKey)
//		  .addHeader("Content-Type", "{Content-Type}")
		  .addHeader("filename", file.getName())
//		  .addHeader("archivepwd", "{archivepwd}")
//		  .addHeader("filepassword", "{filepassword}")
//		  .addHeader("samplesharing", "{samplesharing}")
//		  .addHeader("privateprocessing", "{privateprocessing}")
//		  .addHeader("downloadfrom", "{downloadfrom}")
		  .addHeader("rule", MetadefenderCloudAPI.rule)
//		  .addHeader("sandbox", "{sandbox}")
//		  .addHeader("sandbox_timeout", "{sandbox_timeout}")
//		  .addHeader("sandbox_browser", "{sandbox_browser}")
//		  .addHeader("callbackurl", "{callbackurl}")
//		  .addHeader("rescan_count", "{rescan_count}")
//		  .addHeader("rescan_interval", "{rescan_interval}")
		  .build();

		Response response = null;
		try {
			response = client.newCall(request).execute();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String responseBody = response.body().string();
		response.body().close();
		response.close();
//		log.info("Received response : "+responseBody);
		JSONObject jsonResponse = new JSONObject(responseBody);
		return jsonResponse;
	}
	
	public JSONObject getReport(String dataId) throws IOException
	{
		OkHttpClient client = new OkHttpClient();

		Request request = new Request.Builder()
		  .url(baseUrl+fileUrl+"/"+dataId)
		  .get()
		  .addHeader("apikey", this.apiKey)
//		  .addHeader("x-file-metadata", "{x-file-metadata}")
		  .build();

		Response response = null;
		try {
			response = client.newCall(request).execute();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String responseBody = response.body().string();
		response.body().close();
		response.close();
//		log.info("Received response : "+responseBody);
		JSONObject jsonResponse = new JSONObject(responseBody);
		return jsonResponse;
	}
	
}
