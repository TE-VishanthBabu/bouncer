package com.zorsecyber.bouncer.api.exceptions;

import java.io.Serial;

public class MSGraphException extends Exception {

	@Serial
	private static final long serialVersionUID = 6760326231814970533L;

	public MSGraphException() {
		// TODO Auto-generated constructor stub
	}

	public MSGraphException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public MSGraphException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	public MSGraphException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	public MSGraphException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		// TODO Auto-generated constructor stub
	}

}
