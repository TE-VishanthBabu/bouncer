package com.zorsecyber.bouncer.api.lib.pipelines;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.zorsecyber.bouncer.api.exceptions.PipelineException;
import com.zorsecyber.bouncer.api.lib.MimeTypeUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PipelineUtils {

	private static final ArrayList<ArrayList<String>> mimeTypeClasses = new ArrayList<ArrayList<String>>() {
		/**
		* 
		*/
		private static final long serialVersionUID = -5473879107219672666L;

		{
			add(MimeTypeUtils.archiveMimeTypes);
			add(MimeTypeUtils.singularFileMimeTypes);
		}
	};

	private static final Map<ArrayList<String>, Class<? extends SubmitPipeline>> pipelinesMap = new HashMap<ArrayList<String>, Class<? extends SubmitPipeline>>() {
		/**
		* 
		*/
		private static final long serialVersionUID = 5742195156637187151L;

		{
			put(MimeTypeUtils.singularFileMimeTypes, SingularFilePipeline.class);
			put(MimeTypeUtils.archiveMimeTypes, ArchivePipeline.class);
		}
	};

	private static SubmitPipeline getPipeline(File file, Map<String, Object> metadata) throws PipelineException {
		String mimeType = null;
		String originalFilename = null;
		try {
			long taskId = (long) metadata.get("taskId");
			// see if this is a mailbox and needs to be processed with a graph queue
			if (metadata.containsKey("graphServiceClient"))
				return new MSGraphPipeline(metadata);
			originalFilename = (String) metadata.get("originalFileName");
			mimeType = MimeTypeUtils.stripMimeTypeVersion(MimeTypeUtils.getMimeType(file, originalFilename));
			log.debug("Detected mime type " + mimeType + " for file " + file.getName());
			ArrayList<String> mimeTypeClass = MimeTypeUtils.getMimeTypeClass(mimeType, mimeTypeClasses);
			Class<? extends SubmitPipeline> pipeline = pipelinesMap.get(mimeTypeClass);
			log.info("selected pipeline: " + pipeline.getSimpleName() + " for " + originalFilename + " mimeType="
					+ mimeType);
			return pipeline.getConstructor(Map.class).newInstance(metadata);
		} catch (Exception e) {
//			e.printStackTrace();
			throw new PipelineException(
					"Could not get pipeline for file " + originalFilename + " mimetype=" + mimeType);
		}
	}

	public static Map<String, Object> getAndRunPipeline(File file, Map<String, Object> metadata) throws PipelineException {
		SubmitPipeline pipeline;
		pipeline = PipelineUtils.getPipeline(file, metadata);
		return pipeline.run(file);
	}
}
