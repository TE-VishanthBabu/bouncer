package com.zorsecyber.bouncer.api.lib.extractors;

import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;

import com.zorsecyber.bouncer.api.exceptions.ArchiveExtractorException;
import com.zorsecyber.bouncer.api.lib.WorkerUtils;
import com.zorsecyber.bouncer.api.lib.pipelines.PipelineUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class ZipExtractor implements ArchiveExtractor {

	private static final Map<String, String> fileSystemProperties = new HashMap<String, String>() {
		@Serial
		private static final long serialVersionUID = 2856034736633438754L;
		{
			put("create", "false");
		}
	};
	private FileSystem fs;
	private File workDir;
	public int depth = -1;
	public int numSubmittedFiles = 0;
	public float totalSize = 0;
	@Nonnull private Map<String, Object> metadata;

	public Map<String, Object> extractAndSubmit(File file, File workDir) throws ArchiveExtractorException {
		this.workDir = workDir;
		try {
			log.info("extracting zip file " + file.getName());
			try (FileSystem fileSystem = getFs(file.toPath())) {
				fs = fileSystem;
				extractAndSubmitHelper(fs.getPath("/"));
			}
			Map<String, Object> data = new HashMap<String, Object>();
			data.put("numFiles", numSubmittedFiles);
			data.put("size", totalSize); 
			return data;
		} catch (Exception e) {
//			e.printStackTrace();
			log.warn("Failed to extract and submit from "+file.getAbsolutePath() + ": "+e.getMessage());
			throw new ArchiveExtractorException("Failed to extract and submit from " + file.getAbsolutePath() + ": "+e.getMessage());
		}
	}

	private void extractAndSubmitHelper(Path path) {
		depth++;
		path = fs.getPath(path.toString());
		try {
			if (Files.isDirectory(path)) {
				log.debug("recursing into " + path);
				try {
					Files.list(path).forEach(this::extractAndSubmitHelper);
				} catch (Exception e) {
//					e.printStackTrace();
					throw new ArchiveExtractorException("Could not recurse at depth " + depth + ": " + e.getMessage());
				}
			} else {
				// check out new worker for this file
				UUID fileWorker = WorkerUtils.getNewWorker();
				File file = new File(workDir, fileWorker.toString());
				log.debug("copying file out to "+file.getAbsolutePath().toString());
				Files.copy(path, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
				
				metadata.put("originalFileName", path.getFileName().toString());
				Map<String, Object> data = PipelineUtils.getAndRunPipeline(file, metadata);
				
				int submittedFiles = (int) data.get("numFiles");
					numSubmittedFiles+=submittedFiles;
					totalSize += (float) data.get("size");
				if (numSubmittedFiles > 0 && numSubmittedFiles % 100 == 0) {
					log.info("[" + (long) metadata.get("taskId") + "] Submitted " + numSubmittedFiles + " files");
				}
			}
		} catch (Exception e) {
			log.debug("Exception: " + e.getMessage());
//			e.printStackTrace();
		}
		depth--;
	}

	private FileSystem getFs(Path path) throws IOException {
		return FileSystems.newFileSystem(path, fileSystemProperties);
	}
}
