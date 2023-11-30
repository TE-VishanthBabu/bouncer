package com.zorsecyber.bouncer.core.dao;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import lombok.extern.slf4j.Slf4j;

/**
 * This class represents an Azure storage account.
 */
@Slf4j
public class StorageAccount {

	private static final String configFile = "application.properties";
	private static Properties properties;
	
	public static String datalakeConnectionString() throws IOException {
		properties = new Properties();
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		try (InputStream is = loader.getResourceAsStream(configFile)) {
		  properties.load(is);
		  return properties.getProperty("bouncer.datalake.connection-string");
		} catch (IOException ex) {
			throw new IOException("Could not read " + configFile, ex);
		}
	}

}
