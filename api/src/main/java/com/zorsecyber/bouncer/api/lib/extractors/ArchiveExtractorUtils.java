package com.zorsecyber.bouncer.api.lib.extractors;

import java.io.File;
import java.io.Serial;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.zorsecyber.bouncer.api.exceptions.ArchiveExtractorException;
import com.zorsecyber.bouncer.api.lib.MimeTypeUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ArchiveExtractorUtils {
	private static final ArrayList<ArrayList<String>> mimeTypeClasses = new ArrayList<ArrayList<String>>() {
		@Serial
		private static final long serialVersionUID = -5473879107219672666L;

	{
		add(MimeTypeUtils.zipMimeTypes);
		add(MimeTypeUtils.pstMimeTypes);
	}};
	
	private static final Map<ArrayList<String>, Class<? extends ArchiveExtractor>> extractorsMap = new HashMap<ArrayList<String>,Class<? extends ArchiveExtractor>>() {
		@Serial
		private static final long serialVersionUID = 5742195156637187151L;

	{
	    put(MimeTypeUtils.pstMimeTypes, PstExtractor.class);
	    put(MimeTypeUtils.zipMimeTypes, ZipExtractor.class);
	}};
	
	public static ArchiveExtractor getArchiveExtractor(File file, Map<String, Object> metadata) throws ArchiveExtractorException {
		String mimeType;
		String originalFilename = null;
		try {
			originalFilename = (String) metadata.get("originalFileName");
			mimeType = MimeTypeUtils.stripMimeTypeVersion(MimeTypeUtils.getMimeType(file, originalFilename));
			ArrayList<String> mimeTypeClass = MimeTypeUtils.getMimeTypeClass(mimeType, mimeTypeClasses);
			Class<? extends ArchiveExtractor> extractor = extractorsMap.get(mimeTypeClass);
			log.info("selected extractor "+extractor.getSimpleName()+" for "+originalFilename+" mimeType="+mimeType);
			return extractor.getConstructor(Map.class).newInstance(metadata);
		} catch (Exception e) {
//			e.printStackTrace();
			throw new ArchiveExtractorException("Could not get extractor for archive "+originalFilename+": "+e.getMessage());
		}
	}
}
