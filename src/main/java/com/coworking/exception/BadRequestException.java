package com.coworking.exception;

import lombok.Getter;

@Getter
public class BadRequestException extends RuntimeException {

    private final String code;

    public BadRequestException(String message) {
        super(message);
        this.code = ErrorCode.BAD_REQUEST.name();
    }
}