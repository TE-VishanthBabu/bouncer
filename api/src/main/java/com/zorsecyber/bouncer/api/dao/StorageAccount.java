package com.zorsecyber.bouncer.api.dao;

public class StorageAccount {

	public static final String dataSaConnectionString = System.getenv("APPSETTING_bouncer.storageaccount.connection-string");
	public static final String userDataContainer = System.getenv("APPSETTING_bouncer.containers.userdata");

}
