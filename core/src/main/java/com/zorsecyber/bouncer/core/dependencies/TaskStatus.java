package com.zorsecyber.bouncer.core.dependencies;

public enum TaskStatus {
	FAILED,
	PENDING,
	UPLOADING,
	UPLOADED,
	COMPLETE;
	
	@Override
	public String toString() {
		String name = this.name().toLowerCase();
		return name.substring(0, 1).toUpperCase() + name.substring(1);
	}
}

