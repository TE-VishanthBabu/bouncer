package com.zorsecyber.bouncer.api.functions.queueprocessors;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.persistence.EntityManager;

import org.apache.commons.io.FilenameUtils;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.QueueTrigger;
import com.zorsecyber.bouncer.api.dal.EntityManagerFactorySingleton;
import com.zorsecyber.bouncer.api.dal.TaskDAL;
import com.zorsecyber.bouncer.api.dal.UserDAL;
import com.zorsecyber.bouncer.api.dao.FileSubmission;
import com.zorsecyber.bouncer.api.dao.Task;
import com.zorsecyber.bouncer.api.dao.User;
import com.zorsecyber.bouncer.api.exceptions.ErrorMessages;
import com.zorsecyber.bouncer.api.exceptions.QueueProcessorException;
import com.zorsecyber.bouncer.api.lib.WorkerUtils;
import com.zorsecyber.bouncer.api.lib.pipelines.PipelineUtils;
import com.zorsecyber.bouncer.api.lib.pipelines.SubmitPipeline;
import com.zorsecyber.bouncer.api.lib.status.TaskStatus;
import com.zorsecyber.bouncer.api.lib.storage.AzureBlobContainerClient;
import com.zorsecyber.bouncer.api.lib.storage.AzureFileShareClient;
import com.zorsecyber.bouncer.api.lib.storage.AzureStorageClient;

public class TaskQueueProcessor extends QueueProcessor {
	@SuppressWarnings("null")
	@FunctionName("TaskQueueProcessor")
	public void run(
			@QueueTrigger(name = "message", queueName = "%APPSETTING_bouncer.queue.files%", connection = "APPSETTING_bouncer.storageaccount.connection-string") String message,
			final ExecutionContext context) throws Exception {
		context.getLogger().info("Processing queue message: " + message);

		String[] messageArr = message.split("!");
		long userId = Long.parseLong(messageArr[0]);
		long taskId = Long.parseLong(messageArr[1]);
		String dataSource = messageArr[2];
		Boolean obfuscateFilenames = Boolean.parseBoolean(messageArr[3]);
		String originalFilename;
		String errMsg;
		User user;
		user = UserDAL.getUser(userId);
		if (user == null) {
			context.getLogger().warning("Could not find user " + userId);
			TaskDAL.setTaskStatus(taskId, TaskStatus.FAILED);
			throw new QueueProcessorException("Could not find user " + userId);
		}
		// update task status
		Task task;
		EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
		entityManager.getTransaction().begin();
		try {
			task = TaskDAL.getTask(entityManager, taskId);
			originalFilename = task.getSource();
		} catch (Exception ex) {
			if (entityManager != null && entityManager.getTransaction().isActive()) {
				entityManager.getTransaction().rollback();
			}
			if (entityManager != null) {
				entityManager.close();
			}
			context.getLogger().warning("Could not get task for task" + taskId + ": " + ex.getMessage());
			TaskDAL.setTaskStatus(taskId, TaskStatus.FAILED);
			throw new QueueProcessorException("Could not get task for task" + taskId + ": " + ex.getMessage());
		}
		// delete existing filesubmissions if they exist
		try {
			Collection<FileSubmission> fileSubmissions = task.getFileSubmissions();
			Iterator<FileSubmission> fs = fileSubmissions.iterator();
			if (fileSubmissions.size() > 0) {
				context.getLogger()
						.info("deleting " + fileSubmissions.size() + " existing file submissions for task " + taskId);
				while (fs.hasNext()) {
					entityManager.remove(fs.next());
				}
				TaskDAL.resetTask(task);
				entityManager.getTransaction().commit();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			if (entityManager != null && entityManager.getTransaction().isActive()) {
				entityManager.getTransaction().rollback();
			}
			if (entityManager != null) {
				entityManager.close();
			}
			context.getLogger()
					.warning("Could not delete existing filesubmissions for task" + taskId + ": " + ex.getMessage());
			TaskDAL.setTaskStatus(taskId, TaskStatus.FAILED);
			throw new QueueProcessorException("Could not delete existing filesubmissions for task" + taskId + ": " + ex.getMessage());
		} finally {
			if (entityManager != null && entityManager.getTransaction().isActive()) {
				entityManager.getTransaction().rollback();
			}
			if (entityManager != null) {
				entityManager.close();
			}
		}
		
		// set status to uploading
		TaskDAL.setTaskStatus(taskId, TaskStatus.UPLOADING);

		// get workedId, tempDirName and tempPstName
		File systemTempDir = new File(System.getProperty("java.io.tmpdir"));
		context.getLogger().info("temp dir "+systemTempDir.getAbsolutePath());
		UUID workerId = WorkerUtils.getNewWorker();

		// log to console
		context.getLogger().info("checking out new worker Id: " + workerId);
		// initialize temp pst file
		File file = new File(systemTempDir, workerId.toString()+"."+FilenameUtils.getExtension(originalFilename));
		context.getLogger().info("downloading file " + originalFilename + " to " + file.getAbsolutePath());
		// get pst from source
		try {
			AzureStorageClient storageClient = null;
			if (dataSource.equals("fileshare")) {
				storageClient = new AzureFileShareClient(user);
			} else if (dataSource.equals("blobcontainer")) {
				storageClient = new AzureBlobContainerClient(user);
			}
			storageClient.downloadFileData(originalFilename, file);
		} catch (Exception ex) {
			errMsg = "Could not download file (" + originalFilename + "): " + ex.getMessage();
			context.getLogger().warning(errMsg);
			TaskDAL.setTaskStatus(taskId, TaskStatus.FAILED);
			TaskDAL.setMessage(taskId, ex.getMessage());
			throw new QueueProcessorException(errMsg);
		}

		// extract attachments from pst and submit to nifi
		try {
			if (file == null || !file.exists()) {
				errMsg = "Could not download file (" + originalFilename + ")";
				context.getLogger().warning(errMsg);
				TaskDAL.setTaskStatus(taskId, TaskStatus.FAILED);
				throw new QueueProcessorException(errMsg);
			}
			
			// assemble metadata for pipeline selection
			Map<String, Object> metadata = new HashMap<String, Object>();
			metadata.put("obfuscateFilenames", obfuscateFilenames);
			metadata.put("originalFileName", originalFilename);
			metadata.put("taskId", taskId);

			Map<String, Object> data = PipelineUtils.getAndRunPipeline(file, metadata);
			updateTaskWithResults(task, data, originalFilename);
		} catch (Exception ex) {
			errMsg = this.getClass().getSimpleName()+" failed for " + originalFilename + ": " + ex.getMessage();
			TaskDAL.setTaskStatus(task.getTaskId(), TaskStatus.FAILED);
			TaskDAL.setMessage(taskId, ex.getMessage());
			context.getLogger().warning(errMsg);
			ex.printStackTrace();
			throw new QueueProcessorException(errMsg);
		} finally {
			safeDeleteFile(file);
		}
	}
}
