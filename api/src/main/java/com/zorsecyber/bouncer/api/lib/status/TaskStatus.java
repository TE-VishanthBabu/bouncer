package com.zorsecyber.bouncer.api.lib.status;

public enum TaskStatus {
	FAILED,
	PENDING,
	UPLOADING,
	PROCESSING,
	COMPLETE;
	
	@Override
	public String toString() {
		String name = this.name().toLowerCase();
		return name.substring(0, 1).toUpperCase() + name.substring(1);
	}
}

