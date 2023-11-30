package com.zorsecyber.bouncer.api.lib.storage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;

import com.azure.core.util.Context;
import com.azure.storage.file.share.ShareDirectoryClient;
import com.azure.storage.file.share.ShareFileClient;
import com.zorsecyber.bouncer.api.dao.StorageAccount;
import com.zorsecyber.bouncer.api.dao.User;
import com.zorsecyber.bouncer.api.exceptions.AzureStorageClientException;
import com.zorsecyber.bouncer.api.lib.SubmissionUtils;
import com.zorsecyber.bouncer.api.lib.WorkerUtils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@AllArgsConstructor
public class AzureFileShareClient implements AzureStorageClient {
	// 4MiB upload blocks for azure
	private static final int azure_upload_blockSize = 4194304;
	// 8MiB buffer for saving file data locally
	public static final int DEFAULT_BUFFER_SIZE = 8192;
	private User user;

	public JSONArray listFilesAndDirectories() {
		// handle unset client name
		if (user == null) {
			throw new NullPointerException("user is unset");
		}
		JSONArray list = new JSONArray();
		// connect to the client's file share
		Fileshare clientFs = new Fileshare(user);
		// get the correct directory client
		ShareDirectoryClient dirClient = clientFs.getUserDataDirectoryClient();
		// list files and directories
		dirClient.listFilesAndDirectories().forEach(fileRef -> list.put(fileRef.getName()));
		return list;
	}

	public Boolean fileExists(String fileName) {
		try {
			Fileshare clientFs = new Fileshare(user);
			ShareFileClient fileClient = clientFs.getUserDataFileClient(fileName);

			return fileClient.exists();
		} catch (Exception e) {
			log.warn("exception: " + e.getMessage());
			return false;
		}
	}

	public File downloadFileData(String fileName, File file) throws AzureStorageClientException, IOException {
		// handle unset client name
		if (user == null) {
			throw new NullPointerException("Client name is unset");
		}
		InputStream fileInputStream = null;
		try {
			// connect to the client's file share
			Fileshare clientFs = new Fileshare(user);
			ShareFileClient fileClient = clientFs.getUserDataDirectoryClient().getFileClient(fileName);
			// verify that fileClient is accessible
			Context context = new Context("", "");
			fileClient.existsWithResponse(Duration.ofSeconds((long) Fileshare.shareFileClientTimeout), context);
			log.warn("(" + fileName + ") opening stream");
			// download the file
			fileInputStream = fileClient.openInputStream();
			log.warn("(" + fileName + ") saving to file");
			SubmissionUtils.copyInputStreamToFile(fileInputStream, file);
			log.warn("(" + fileName + ") saved to file");
			// close any open handles
			log.info("closing handles");
			fileClient.listHandles().forEach(handle -> {
				log.info("closing handle " + handle.getPath());
				fileClient.forceCloseHandle(handle.getHandleId());
			});
			return file;
		} catch (Exception e) {
			log.warn("Exception: " + e.getMessage());
			e.printStackTrace();
			throw new AzureStorageClientException("Could not download file " + fileName + ": " + e.getMessage());
		} finally {
			// close stream
			if (fileInputStream != null) {
				fileInputStream.close();
			}
		}
	}
	
	public static Boolean uploadToInFileShare(File file) {
		ByteArrayInputStream fileDataStream = null;
		try {
			byte[] fileData = FileUtils.readFileToByteArray(file);
			fileDataStream = new ByteArrayInputStream(fileData);
			ShareDirectoryClient tempDir = Fileshare.getIngestTempDirectoryClient();
			ShareFileClient tempFileClient = Fileshare.getIngestFileClient(Fileshare.ingestFileshareTempDirectory + "/" + file.getName());
			
			tempDir.createIfNotExists();
			// create temp file
			tempDir.deleteFileIfExists(file.getName());
			tempDir.createFile(file.getName(), fileData.length);
			
			tempFileClient.create(fileData.length);
			if (tempFileClient.exists()) {
				ByteArrayInputStream chunk;
				while (true) {
					byte[] buffer = fileDataStream.readNBytes(azure_upload_blockSize);
					if (buffer.length == 0) {
						break;
					}
					chunk = new ByteArrayInputStream(buffer);
					tempFileClient.uploadRange(chunk, buffer.length);
					chunk.close();
				}
				// rename file to "move" it from temp dir
				tempFileClient.rename(file.getName());
				return true;
			} else {
				throw new Exception("could not create file in inFileShare");
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			log.warn("exception: " + ex.getMessage());
			return false;
		} finally {
			if (fileDataStream != null) {
				try {
					fileDataStream.close();
				} catch (IOException ex2) {
				}
			}
		}
	}
	
	public static boolean createUserDataDir(User user) {
		ShareDirectoryClient userDataFs = Fileshare.getDirectoryClient(StorageAccount.dataSaConnectionString,
				StorageAccount.userDataContainer, Long.toString(user.getUserId()));
		if(!userDataFs.exists()) {
			userDataFs.create();
		}
		return userDataFs.exists();
	}

}
