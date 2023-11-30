package com.zorsecyber.bouncer.core.dao;

import java.io.IOException;

import com.azure.storage.file.share.ShareClient;
import com.azure.storage.file.share.ShareDirectoryClient;
import com.azure.storage.file.share.ShareFileClient;
import com.azure.storage.file.share.ShareFileClientBuilder;

/**
 * This class builds a ShareFileClient or ShareDirectoryClient pointing to a specified
 * file or directory name inside a file share belonging to the the siaft Datalake storage account
 */

public class FileShare {
	
	public static ShareFileClient getDatalakeFileClient(String fileName, String shareName) throws IOException
	{
		return new ShareFileClientBuilder().connectionString(StorageAccount.datalakeConnectionString())
				.shareName(shareName).resourcePath(fileName).buildFileClient();
	}

}
