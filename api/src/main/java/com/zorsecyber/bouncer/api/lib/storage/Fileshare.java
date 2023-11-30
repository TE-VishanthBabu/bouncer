package com.zorsecyber.bouncer.api.lib.storage;

import com.azure.storage.file.share.ShareDirectoryClient;
import com.azure.storage.file.share.ShareFileClient;
import com.azure.storage.file.share.ShareFileClientBuilder;
import com.zorsecyber.bouncer.api.dao.StorageAccount;
import com.zorsecyber.bouncer.api.dao.User;

public class Fileshare {
	public static final String ingestFileshare = System.getenv("APPSETTING_bouncer.containers.ingest");
	public static final String ingestFileshareTempDirectory = "temp";
	public static final long shareFileClientTimeout = 60; // seconds
	private User user;
	
	public Fileshare(User user) {
		this.user = user;
		getUserDataDirectoryClient().createIfNotExists();
	}

	/**
	 * This method returns a sharefileclient object.
	 * 
	 * @param connectionString Connection string for the target storage account
	 * @param shareName        Name of the target fileshare
	 * @param fileName         Name of the target file
	 * @return ShareFileClient A ShareFileClient pertaining to the target file
	 */
	public static ShareFileClient getFileClient(String connectionString, String shareName, String fileName) {
		return new ShareFileClientBuilder().connectionString(connectionString).shareName(shareName)
				.resourcePath(fileName).buildFileClient();
	}

	public static ShareDirectoryClient getDirectoryClient(String connectionString, String shareName,
			String dirName) {
		return new ShareFileClientBuilder().connectionString(connectionString).shareName(shareName)
				.resourcePath(dirName).buildDirectoryClient();
	}

	public ShareFileClient getUserDataFileClient(String fileName) {
		return getUserDataDirectoryClient().getFileClient(fileName);
	}
	
	public static ShareDirectoryClient getIngestTempDirectoryClient() {
		return getDirectoryClient(StorageAccount.dataSaConnectionString, Fileshare.ingestFileshare, ingestFileshareTempDirectory);
	}

	public static ShareFileClient getIngestFileClient(String fileName) {
		return getFileClient(StorageAccount.dataSaConnectionString, Fileshare.ingestFileshare,
				fileName);
	}

	public ShareDirectoryClient getUserDataDirectoryClient() {
		return getDirectoryClient(StorageAccount.dataSaConnectionString,
				StorageAccount.userDataContainer, Long.toString(user.getUserId()));
	}

}
