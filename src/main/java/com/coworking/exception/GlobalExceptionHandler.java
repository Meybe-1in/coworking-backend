package com.coworking.exception;

import com.coworking.dto.common.ApiResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // VALIDACIONES (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Datos inválidos");

        return buildError(
                HttpStatus.BAD_REQUEST,
                message,
                request,
                ErrorCode.BAD_REQUEST.name()
        );
    }

    // BAD REQUEST
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> handleBadRequest(
            BadRequestException ex,
            HttpServletRequest request) {

        return buildError(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request,
                ex.getCode()
        );
    }


    //BAD CREDENTIAL AUTH
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(
            HttpServletRequest request) {

        return buildError(
                HttpStatus.UNAUTHORIZED,
                "Credenciales incorrectas",
                request,
                ErrorCode.UNAUTHORIZED.name()
        );
    }

    // NOT FOUND
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            NotFoundException ex,
            HttpServletRequest request) {

        return buildError(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request,
                ex.getCode()
        );
    }

    @ExceptionHandler(RoomHasReservationsException.class)
    public ResponseEntity<ApiError> handleRoomHasReservations(
            RoomHasReservationsException ex,
            HttpServletRequest request) {

        return buildError(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                request,
                ErrorCode.CONFLICT.name()
        );
    }

    // CONFLICT (reservas)
    @ExceptionHandler(ReservationConflictException.class)
    public ResponseEntity<ApiError> handleConflict(
            ReservationConflictException ex,
            HttpServletRequest request) {

        return buildError(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                request,
                ex.getCode()
        );
    }

    //FALTA PARAMETRO
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParam(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {

        return buildError(
                HttpStatus.BAD_REQUEST,
                ex.getParameterName() + " es requerido",
                request,
                ErrorCode.BAD_REQUEST.name()
        );
    }

    // ARGUMENTO INVALIDO
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        return buildError(
                HttpStatus.BAD_REQUEST,
                "Parámetro inválido",
                request,
                ErrorCode.BAD_REQUEST.name()
        );
    }

    // ERRORES GENERALES
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneral(
            Exception ex,
            HttpServletRequest request) {

        return buildError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error interno del servidor",
                request,
                ErrorCode.INTERNAL_ERROR.name()
        );
    }

    // BUILDER CENTRAL
    private ResponseEntity<ApiError> buildError(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            String code) {

        ApiError error = ApiError.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .code(code)
                .build();

        return ResponseEntity.status(status).body(error);
    }
}
