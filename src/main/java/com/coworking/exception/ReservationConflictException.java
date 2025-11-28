package com.coworking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ReservationConflictException extends RuntimeException{

    public ReservationConflictException(String message) {
        super(message);
    }
}
