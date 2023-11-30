
package com.zorsecyber.bouncer.api.lib.pipelines;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.zorsecyber.bouncer.api.exceptions.PipelineException;
import com.zorsecyber.bouncer.api.lib.SubmissionUtils;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class SingularFilePipeline implements SubmitPipeline {

	private Map<String, Object> metadata;

	public Map<String, Object> run(File file) throws PipelineException {
		long taskId = (long) metadata.get("taskId");
		boolean success = false;
		float size = 0;
		int numSubmittedFiles = 0;
		try {
			long fileLength = file.length();
			log.debug("submitting file " + file.getName() + " length " + fileLength);
			Boolean obfuscateFilename = (Boolean) metadata.get("obfuscateFilenames");

			String originalFilename = (String) metadata.get("originalFileName");
			success = SubmissionUtils.submitLocalFile(file, taskId, obfuscateFilename, originalFilename);
			if (file != null && file.exists()) {
				log.debug("deleting file " + originalFilename);
				file.delete();
			}

			if (success) {
				numSubmittedFiles++;
				size += (fileLength / Math.pow(10, 9));
			}
			log.debug("size " +size);
			Map<String, Object> data = new HashMap<String, Object>();
			data.put("numFiles", numSubmittedFiles);
			data.put("size", size);
			
			return data;
		} catch (Exception e) {
//			e.printStackTrace();
			throw new PipelineException("Unable to run SingularFilePipeline for taskId " + taskId);
		} finally {
			if (file != null && file.exists()) {
				file.delete();
			}
		}
	}

}
