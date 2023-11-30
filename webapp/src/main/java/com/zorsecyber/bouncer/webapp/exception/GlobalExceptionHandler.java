package com.zorsecyber.bouncer.webapp.exception;

import java.util.HashMap;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.zorsecyber.bouncer.webapp.constant.Constant;
import com.zorsecyber.bouncer.webapp.response.CommonResponse;
import com.zorsecyber.bouncer.webapp.response.ExceptionResponse;

@ControllerAdvice
public class GlobalExceptionHandler {
    @Autowired
    private MessageSource messageSource;

    @ExceptionHandler(EmailNotSentException.class)
    public ResponseEntity<CommonResponse> handleEmailNotSentException(EmailNotSentException ex) {
        HashMap<String, String> response = new HashMap<>();
        response.put(Constant.DETAILS, messageSource.getMessage("email.failed", null, Locale.ENGLISH));
        CommonResponse error = new CommonResponse(Constant.STATUS_FAILED, HttpStatus.CONFLICT.value(), ex.getMessage(), response);
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(VerificationFailedException.class)
    public ResponseEntity<CommonResponse> handleVerificationFailedException(VerificationFailedException ex) {
        HashMap<String, String> response = new HashMap<>();
        response.put(Constant.DETAILS, messageSource.getMessage("verification.notDone",null, Locale.ENGLISH));
        CommonResponse error = new CommonResponse(Constant.STATUS_FAILED, HttpStatus.NOT_FOUND.value(), ex.getMessage(), response);
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<CommonResponse> handleUserNotFoundException(UserNotFoundException ex) {
        HashMap<String, String> response = new HashMap<>();
        response.put(Constant.DETAILS, messageSource.getMessage("user.notFound",null, null));
        CommonResponse error = new CommonResponse(Constant.STATUS_FAILED, HttpStatus.NOT_FOUND.value(), ex.getMessage(), response);
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(CommonException.class)
    public ResponseEntity<ExceptionResponse> handleExceptions(CommonException ex) {
        ExceptionResponse response = new ExceptionResponse();
        response.setCode(ex.getStatus().value());
        response.setStatus(ex.getStatus().getReasonPhrase());
        response.setMessage(ex.getMessage());
        response.setDetailedError(ex.getDetailedError());
        return new ResponseEntity<>(response, ex.getStatus());
    }
}
