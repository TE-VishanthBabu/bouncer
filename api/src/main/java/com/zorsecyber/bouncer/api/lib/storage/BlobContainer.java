package com.zorsecyber.bouncer.api.lib.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobClientBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.zorsecyber.bouncer.api.dao.StorageAccount;
import com.zorsecyber.bouncer.api.dao.User;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * This class builds blobclient and blobcontainerclient objects belonging to a client's
 * private blob container within the siaftdatalakesa storage account.
 * @param user	Name of the client
 */
@Slf4j
@AllArgsConstructor
public class BlobContainer {
	public static final long BLOB_READ_TIMEOUT = 60; // seconds
	private User user;
	
	  /**
	   * This method returns a blobclient pertaining to a blob within a client's
	   * private blob container.
	   * @param blobName	The name of the blob
	   * @return BlobClient	The blobclient pertaining to the blob with name blobName
	   */
	public BlobClient getBlobClient(String blobName)
	{
		String blobPath = user.getUserId() + "/" + blobName;
		log.info("getting "+blobPath);
		return new BlobClientBuilder().connectionString(StorageAccount.dataSaConnectionString)
				.containerName(StorageAccount.userDataContainer).blobName(blobPath).buildClient();
	}
	
	public BlobClient getBlobClient() {
		return new BlobClientBuilder().connectionString(StorageAccount.dataSaConnectionString)
				.containerName(StorageAccount.userDataContainer).blobName(Long.toString(user.getUserId())).buildClient();
	}
	
	  /**
	   * This method returns a blobcontainerclient pertaining to a client's 
	   * private blob container.
	   * @return BlobContainerClient
	   * 			The blobcontainerclient pertaining to the client's blobcontainer
	   */
	public BlobContainerClient getUserDataBlobContainerClient()
	{
		return new BlobContainerClientBuilder().connectionString(StorageAccount.dataSaConnectionString)
				.containerName(StorageAccount.userDataContainer).buildClient();
	}
	
}