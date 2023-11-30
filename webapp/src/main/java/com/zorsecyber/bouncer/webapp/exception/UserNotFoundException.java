package com.zorsecyber.bouncer.webapp.exception;

import lombok.Data;

@Data
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
