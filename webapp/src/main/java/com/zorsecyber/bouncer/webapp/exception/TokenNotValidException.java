package com.zorsecyber.bouncer.webapp.exception;

import lombok.Data;

@Data
public class TokenNotValidException extends RuntimeException{
    public TokenNotValidException(String message) {
        super(message);
    }
}
