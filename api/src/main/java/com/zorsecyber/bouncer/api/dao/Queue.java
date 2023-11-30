package com.zorsecyber.bouncer.api.dao;

public class Queue {
	public static final String filesQueueName = System.getenv("APPSETTING_bouncer.queue.files");
	public static final String graphQueueName = System.getenv("APPSETTING_bouncer.queue.graph");
}
