package com.zorsecyber.bouncer.api.lib.storage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.MessageDigestAlgorithms;
import org.json.JSONArray;

import com.zorsecyber.bouncer.api.exceptions.AzureStorageClientException;

public interface AzureStorageClient {
	public JSONArray listFilesAndDirectories();
	public Boolean fileExists(String fileName);
	public File downloadFileData(String fileName, File file) throws AzureStorageClientException, IOException;
	
	public static String sha256(File file) throws AzureStorageClientException {
		try {
			byte[] digest = new DigestUtils(MessageDigestAlgorithms.SHA_256).digest(file);
			return bytesToHex(digest);
		} catch (Exception e) {
			throw new AzureStorageClientException("Could not compute sha256: " + e.getMessage());
		}
	}
	public static String md5(File file) throws AzureStorageClientException {
		try {
			byte[] digest = new DigestUtils(MessageDigestAlgorithms.MD5).digest(file);
			return bytesToHex(digest);
		} catch (Exception e) {
			throw new AzureStorageClientException("Could not compute md5: " + e.getMessage());
		}
	}
	
	public static String bytesToHex(byte[] hash) {
		StringBuilder hexString = new StringBuilder(2 * hash.length);
		for (int i = 0; i < hash.length; i++) {
			String hex = Integer.toHexString(0xff & hash[i]);
			if (hex.length() == 1) {
				hexString.append('0');
			}
			hexString.append(hex);
		}
		return hexString.toString();
	}

	public static ArrayList<String> jsonArrayToList1d(JSONArray array) {
		ArrayList<String> l = new ArrayList<String>();
		for (int i = 0; i < array.length(); i++) {
			l.add(array.getString(i));
		}
		return l;
	}
}
