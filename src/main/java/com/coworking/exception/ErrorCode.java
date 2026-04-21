package com.coworking.exception;

public enum ErrorCode {
    // reservas
    RESERVATION_OVERLAP,
    DUPLICATE_RESERVATION,
    INVALID_TIME_RANGE,
    INVALID_DURATION,
    PAST_TIME_NOT_ALLOWED,

    // generales
    NOT_FOUND,
    BAD_REQUEST,
    UNAUTHORIZED,
    INTERNAL_ERROR
}
