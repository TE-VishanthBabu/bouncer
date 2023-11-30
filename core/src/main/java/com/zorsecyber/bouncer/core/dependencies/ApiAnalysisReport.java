package com.zorsecyber.bouncer.core.dependencies;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.UserPrincipal;

import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.io.OutputStreamCallback;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.controlcyberrisk.siaft.dependencies.HttpUtils;
import com.zorsecyber.bouncer.core.dal.ReportRepoDAL;

public class ApiAnalysisReport {
	protected static final Logger log = LoggerFactory.getLogger(ApiAnalysisReport.class);
	
	public JSONObject json = new JSONObject();
	public String sha256 = new String();

	public ApiAnalysisReport(JSONObject raw) {
		json = raw;
	}
	
	public FlowFile writeContentToFlowFile(ProcessSession session, FlowFile file) {
//			log.info("writing content: ");
//			log.info(json.toString());
			return session.write(file, new OutputStreamCallback() {

		        @Override
		        public void process(OutputStream out) throws IOException {
		        	out.write(json.toString().getBytes(StandardCharsets.UTF_8));
		        	out.close();
		        }
		    });
		}
	
	public boolean saveReportToFile(String dir) {
		String datadirOwner = "nifi";
		dir = HttpUtils.sanitize_path(dir);
		
		File directory = new File(HttpUtils.sanitize_path(dir));
		if (!directory.exists()) {
			log.warn("persistDir " + dir + " does not exist ");
			// set owner
			Path output_dir_path = Paths.get(dir);
			try {
				UserPrincipal output_dir_owner = output_dir_path
						.getFileSystem()
						.getUserPrincipalLookupService()
						.lookupPrincipalByName(datadirOwner);	
				Files.setOwner(output_dir_path, output_dir_owner);
			} catch (Exception e) {
				log.error("failed to create directory "+dir+" : "+e.getMessage());
				return false;
			}
		}
		PrintWriter writer;
		try {
			writer = new PrintWriter(HttpUtils.sanitize_path(dir) + sha256 + ".json", "UTF-8");
			writer.println(json.toString());
			writer.close();
			log.debug("Saved report to " + HttpUtils.sanitize_path(dir) + sha256 + ".json");
	
		} catch (Exception e) {
			log.error("Could not save json report : "+e.getMessage());
			return false;
		}
		return true;
	}
	
	public static ApiAnalysisReport getReportFromFile(String sha256, String dataDir, String analysisType) throws Exception
	{
		/** generate an SIreport object from JSON **/
		return new ApiAnalysisReport(ReportRepoDAL.getReportJson(sha256, dataDir, analysisType));
	}

}
