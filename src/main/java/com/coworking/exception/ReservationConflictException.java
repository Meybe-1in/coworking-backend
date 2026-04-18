package com.coworking.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@Getter
public class ReservationConflictException extends RuntimeException {

    private final String code;

    public ReservationConflictException(String code, String message) {
        super(message);
        this.code = code;
    }

}
