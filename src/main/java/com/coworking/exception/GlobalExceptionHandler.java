package com.coworking.exception;

import com.coworking.storage.exception.InvalidStorageFileException;
import com.coworking.storage.exception.StorageException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // VALIDACIONES (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error ->
                        error.getField() + ": " + error.getDefaultMessage()
                )
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

    // BAD CREDENTIAL AUTH
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

    // ROOM HAS RESERVATIONS
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

    // RESERVATION CONFLICT
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

    // MISSING PARAMETER
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

    // INVALID ARGUMENT
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

    // INVALID IMAGE
    @ExceptionHandler(InvalidStorageFileException.class)
    public ResponseEntity<ApiError> handleInvalidStorageFile(
            InvalidStorageFileException ex,
            HttpServletRequest request) {

        return buildError(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request,
                ErrorCode.BAD_REQUEST.name()
        );
    }

    // FILE EXCEEDS MULTIPART LIMIT
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException ex,
            HttpServletRequest request) {

        return buildError(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "La imagen supera el tamaño máximo permitido",
                request,
                ErrorCode.BAD_REQUEST.name()
        );
    }

    // MULTIPART PROCESSING ERROR
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiError> handleMultipartException(
            MultipartException ex,
            HttpServletRequest request) {

        log.warn("Error procesando archivo multipart", ex);

        return buildError(
                HttpStatus.BAD_REQUEST,
                "No fue posible procesar el archivo enviado",
                request,
                ErrorCode.BAD_REQUEST.name()
        );
    }

    // SUPABASE STORAGE ERROR
    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ApiError> handleStorageException(
            StorageException ex,
            HttpServletRequest request) {

        log.error("Error al comunicarse con Supabase Storage", ex);

        return buildError(
                HttpStatus.BAD_GATEWAY,
                "No fue posible almacenar la imagen. Intenta nuevamente.",
                request,
                ErrorCode.STORAGE_ERROR.name()
        );
    }

    // GENERAL ERRORS
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneral(
            Exception ex,
            HttpServletRequest request) {

        log.error("Error interno no controlado", ex);

        return buildError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error interno del servidor",
                request,
                ErrorCode.INTERNAL_ERROR.name()
        );
    }

    // CENTRAL ERROR BUILDER
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
