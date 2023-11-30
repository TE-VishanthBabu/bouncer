package com.zorsecyber.bouncer.api.functions.queueprocessors;

import java.io.File;
import java.util.Map;

import com.zorsecyber.bouncer.api.dal.TaskDAL;
import com.zorsecyber.bouncer.api.dao.Task;
import com.zorsecyber.bouncer.api.exceptions.QueueProcessorException;
import com.zorsecyber.bouncer.api.lib.status.TaskStatus;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QueueProcessor {
	protected void updateTaskWithResults(Task task, Map<String, Object> data, String source)
			throws QueueProcessorException {
		try {
			int numSubmittedFiles = (int) data.get("numFiles");
			float size = (float) data.get("size");
			long taskId = task.getTaskId();
			// indicate that no files were submitted
			if(numSubmittedFiles == 0) {
				numSubmittedFiles = -1;
			}
			log.info("Submitted " + numSubmittedFiles + " files from " + source + ". Total size: " + size);
			// set number of files
			if (TaskDAL.setNumberOfFiles(taskId, numSubmittedFiles)) {
				log.info("Set number of files for " + source);
			} else {
				log.warn("Failed to set number of files for " + source);
			}
			// set size
			if (TaskDAL.setSize(taskId, size)) {
				log.info("Set size for " + source);
			} else {
				log.warn("Failed to set size for " + source);
			}
			// set status
			if (TaskDAL.setTaskStatus(taskId, TaskStatus.PROCESSING)) {
				log.info("Set task status for " + source);
			} else {
				log.warn("failed to set task status for " + source);
			}
		} catch (Exception ex) {
			throw new QueueProcessorException(ex);
		}
	}
	
	protected void safeDeleteFile(File file) {
		if(file != null && file.exists()) {
			file.delete();
		}
	}
}
