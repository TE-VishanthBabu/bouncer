package com.zorsecyber.bouncer.webapp.exception;

import lombok.Data;

@Data
public class EmailNotSentException extends RuntimeException{

    public EmailNotSentException(String message) {
        super(message);
    }
}
