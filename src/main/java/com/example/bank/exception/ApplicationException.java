package com.example.bank.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApplicationException extends RuntimeException {
    private final HttpStatus status;

    public ApplicationException(String message) {
        this(message, HttpStatus.BAD_REQUEST);
    }

    public ApplicationException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

}
