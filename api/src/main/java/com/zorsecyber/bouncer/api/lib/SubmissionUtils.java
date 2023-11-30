package com.zorsecyber.bouncer.api.lib;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FilenameUtils;
import org.codehaus.plexus.util.StringUtils;

import com.google.common.io.Files;
import com.zorsecyber.bouncer.api.dal.FileSubmissionDAL;
import com.zorsecyber.bouncer.api.dao.FileSubmission;
import com.zorsecyber.bouncer.api.lib.storage.AzureFileShareClient;
import com.zorsecyber.bouncer.api.lib.storage.AzureStorageClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SubmissionUtils {
	private static final String SUBID_DELIMITER = "_";

	public static Boolean submitLocalFile(File file, Long taskId, Boolean obfuscateFilename) throws IOException {
		return submitLocalFile(file, taskId, obfuscateFilename, null);
	}

	public static Boolean submitLocalFile(File file, Long taskId, Boolean obfuscateFilename, String originalFilename)
			throws IOException {
		try {
			if (!MimeTypeUtils.allowedMimeType(file)) {
				return false;
			}
			String sha256 = AzureStorageClient.sha256(file);
			String filename;
			String uuidFilename;
			String extension;
			
			extension = FilenameUtils.getExtension(originalFilename);
			// determine filename
			if (obfuscateFilename) {
				filename =  AzureStorageClient.md5(file).toString();
				if(!extension.isEmpty()) {
				filename += "." + extension;
				}
				log.debug("obfuscating original name="+originalFilename+" -> "+filename);
			} else if (originalFilename != null) {
				if (!StringUtils.isEmpty(originalFilename))
					filename = originalFilename;
				else
					filename = "(empty filename)";
			} else {
				filename = file.getName();
			}
			FileSubmissionDAL fileSubmissionDAL = new FileSubmissionDAL();
			if (!FileSubmissionDAL.checkFileSubExists(sha256, taskId)) {
				log.debug("creating filesubmission "+taskId+", "+filename+", "+sha256);
				FileSubmission fileSub = fileSubmissionDAL.createFileSubmission(taskId, filename, sha256);
				// check that the submission persisted to the db
				if (fileSub.getSubmissionId() > 0) {
					// create filename from uuid to prevent collisions in ingest directory
					uuidFilename = WorkerUtils.getNewWorker().toString();
					if(!extension.isEmpty()) {
						uuidFilename += "." + extension;
					}
					log.debug("uploading " + sha256 + " (" + filename + ") with submissionId " + fileSub.getSubmissionId());
					// rename file using uuid
					File fileToUpload = new File(file.getParentFile() + File.separator
							+ Long.toString(fileSub.getSubmissionId()) + SUBID_DELIMITER + uuidFilename);
					log.trace("renaming " + file.getName() + " to : " + fileToUpload.getName());
					if(fileToUpload.exists()) {
						log.debug("fileToUpload already exists");
					}
					Files.copy(file, fileToUpload);
					if(fileToUpload.length() == 0) {
						log.warn("file length is zero: "+fileToUpload.getName());
					}
					Boolean success = AzureFileShareClient.uploadToInFileShare(fileToUpload);
					fileToUpload.delete();
					return success;
				}
			}
			log.debug("duplicate hash " + sha256);
			return false;
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
	}

	public static void copyInputStreamToFile(InputStream in, File file) throws Exception {
		try (FileOutputStream outputStream = new FileOutputStream(file, false)) {
			int read;
			int numWrites = 0;
			byte[] bytes = new byte[AzureFileShareClient.DEFAULT_BUFFER_SIZE];
			while ((read = in.read(bytes)) != -1) {
				outputStream.write(bytes, 0, read);
				numWrites += 1;
				if (numWrites % (12800) == 0) {
					System.out.println("Wrote " + numWrites / 128 + "MiB");
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new Exception("Could not download file "+file.getName(), ex);
		}
	}

}
