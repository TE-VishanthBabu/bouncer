package com.zorsecyber.bouncer.webapp.exception;

import lombok.Data;

@Data
public class VerificationFailedException extends RuntimeException{
    public VerificationFailedException(String message) {
        super(message);
    }
}
