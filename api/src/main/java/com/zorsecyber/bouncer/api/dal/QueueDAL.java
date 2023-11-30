package com.zorsecyber.bouncer.api.dal;

import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.QueueClientBuilder;
import com.azure.storage.queue.models.QueueStorageException;

public class QueueDAL {
	
	public QueueDAL()
	{
		
	}
	
	public static QueueClient createQueue(String queueName, String connectStr)
	{
	    try
	    {
	        // Instantiate a QueueClient which will be
	        // used to create and manipulate the queue
	        QueueClient queue = new QueueClientBuilder()
	                                .connectionString(connectStr)
	                                .queueName(queueName)
	                                .buildClient();
	        // Create the queue
	        queue.create();
	        return queue;
	    }
	    catch (QueueStorageException e)
	    {
	        // Output the exception message and stack trace
	        System.out.println("Error code: " + e.getErrorCode() + "Message: " + e.getMessage());
	        return null;
	    }
	}
}
