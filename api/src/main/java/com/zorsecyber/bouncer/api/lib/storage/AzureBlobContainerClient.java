package com.zorsecyber.bouncer.api.lib.storage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

import org.json.JSONArray;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobListDetails;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.zorsecyber.bouncer.api.dao.User;
import com.zorsecyber.bouncer.api.exceptions.AzureStorageClientException;
import com.zorsecyber.bouncer.api.lib.SubmissionUtils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
@AllArgsConstructor
public class AzureBlobContainerClient implements AzureStorageClient {
	private User user;

	public JSONArray listFilesAndDirectories() {
		JSONArray list = new JSONArray();
		BlobContainer clientBc = new BlobContainer(user);
		BlobContainerClient clientBcClient = clientBc.getUserDataBlobContainerClient();
		ListBlobsOptions options = new ListBlobsOptions()
			     .setPrefix(Long.toString(user.getUserId())+"/")
			     .setDetails(new BlobListDetails()
			         .setRetrieveDeletedBlobs(false)
			         .setRetrieveSnapshots(false));
		clientBcClient.listBlobsByHierarchy("/", options, Duration.ofSeconds(BlobContainer.BLOB_READ_TIMEOUT))
				.forEach(blob -> 
				list.put(formatBlobRef(blob))
				);
		return list;
	}
	
	private String formatBlobRef(BlobItem blob) {
		String name = blob.getName();
		int lastSep = name.lastIndexOf("/");
		if(lastSep != -1 && lastSep + 1 < name.length()) {
			// +1 to remove leading /
			return name.substring(lastSep + 1);
		}
		return blob.getName();
	}

	public Boolean fileExists(String fileName) {
		BlobContainer blobContainer = new BlobContainer(user);
		BlobClient blobClient = blobContainer.getBlobClient(fileName);
		return blobClient.exists();
	}

	public File downloadFileData(String fileName, File file) throws AzureStorageClientException, IOException {
		InputStream fileInputStream = null;
		try {
			BlobContainer blobContainer = new BlobContainer(user);
			BlobClient blobClient = blobContainer.getBlobClient(fileName);
			System.out.println("- opening stream");
			fileInputStream = blobClient.openInputStream();
			System.out.println("- saving to file");
			SubmissionUtils.copyInputStreamToFile(fileInputStream, file);
			return file;
		} catch (Exception e) {
			throw new AzureStorageClientException("Could not download file " + fileName + ": " + e.getMessage());
		} finally {
			// close stream
						if (fileInputStream != null) {
							fileInputStream.close();
						}
		}
	}

}
