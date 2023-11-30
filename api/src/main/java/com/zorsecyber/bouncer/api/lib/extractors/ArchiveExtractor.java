package com.zorsecyber.bouncer.api.lib.extractors;

import java.io.File;
import java.util.Map;

import com.zorsecyber.bouncer.api.exceptions.ArchiveExtractorException;

public interface ArchiveExtractor {
	public Map<String, Object> extractAndSubmit(File file, File workDir) throws ArchiveExtractorException;
}
