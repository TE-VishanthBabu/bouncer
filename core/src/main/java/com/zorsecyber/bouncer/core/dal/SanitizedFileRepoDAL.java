package com.zorsecyber.bouncer.core.dal;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azure.storage.file.share.ShareFileClient;
import com.controlcyberrisk.siaft.dependencies.HttpUtils;
import com.zorsecyber.bouncer.core.dao.FileShare;
import com.zorsecyber.bouncer.core.dao.SanitizedFileRepo;
import com.zorsecyber.bouncer.core.dependencies.SophosIntelixReport;

/**
 * This contains methods used to interact with the Report Repo
 */
public class SanitizedFileRepoDAL {
	private static final Logger log = LoggerFactory.getLogger(ReportRepoDAL.class);
	// 4MiB blocks
	public static final int DEFAULT_BUFFER_SIZE = 8192; 

	public SanitizedFileRepoDAL() {
	}

	  /**
	   * This method checks whether a specific analysis report exists in the repo.
	   * @param sha256 The sha256 checksum of the file.
	   * @param analysisType The type of report to look for.
	   * @return Boolean yes if report exists.
	   */
	public static boolean sanitizedFileInRepo(String sha256, String provider)
	{
		String fileName = provider+"/"+sha256;
		log.info("Looking for "+fileName);
		System.out.println("Looking for "+fileName);
		try {
			ShareFileClient fileClient = FileShare.getDatalakeFileClient(fileName, SanitizedFileRepo.SanitizedFilesDataLakeFSName);
			return fileClient.exists();
		} catch (Exception e) {
			System.out.println("exception: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}
}
