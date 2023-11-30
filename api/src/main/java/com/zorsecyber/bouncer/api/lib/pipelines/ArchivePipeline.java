package com.zorsecyber.bouncer.api.lib.pipelines;

import java.io.File;
import java.util.Map;

import com.pff.PSTFile;
import com.zorsecyber.bouncer.api.exceptions.PipelineException;
import com.zorsecyber.bouncer.api.lib.extractors.ArchiveExtractor;
import com.zorsecyber.bouncer.api.lib.extractors.ArchiveExtractorUtils;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class ArchivePipeline implements SubmitPipeline {
	private Map<String, Object> metadata;

	public Map<String, Object> run(File file) throws PipelineException {
		long taskId = (long) metadata.get("taskId");
		File systemTempDir = new File(System.getProperty("java.io.tmpdir"));
		File tempDir = new File(systemTempDir, file.getName() + "-work");
		// create work dir for this workerId
		if (tempDir.exists() && tempDir.isDirectory()) {
			tempDir.delete();
		}
		log.debug("creating work dir " + tempDir.getName());
		tempDir.mkdirs();
		PSTFile pstFile = null;
		try {
			ArchiveExtractor extractor = ArchiveExtractorUtils.getArchiveExtractor(file, metadata);
			log.debug("extracting " + file.getName());
			Map<String, Object> data = extractor.extractAndSubmit(file, tempDir);
			return data;
		} catch (Exception e) {
//			e.printStackTrace();
			throw new PipelineException("Unable to run ArchivePipeline for taskId " + taskId + ": " +e.getMessage());
		} finally {
			// clean up temp pst and dir
			if (pstFile != null) {
				try {
					pstFile.close();
				} catch (Exception e) {
					throw new PipelineException(e);
				}
			}
			if (tempDir.exists()) {
				tempDir.delete();
				log.debug("Deleted work dir " + tempDir.getName());
			}
			if (file != null && file.exists()) {
				file.delete();
				log.debug("Deleted file " + file.getName());
			}
		}
	}
}
