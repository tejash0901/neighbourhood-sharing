package com.neighborshare.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @Data
    @AllArgsConstructor
    public static class ErrorResponse {
        private String timestamp;
        private int status;
        private String error;
        private String errorCode;
        private String message;
        private String path;
        private Map<String, String> fieldErrors;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
        ResourceNotFoundException ex, WebRequest request) {

        ErrorResponse response = new ErrorResponse(
            LocalDateTime.now().toString(),
            HttpStatus.NOT_FOUND.value(),
            "Resource Not Found",
            ex.getErrorCode(),
            ex.getMessage(),
            request.getDescription(false).replace("uri=", ""),
            null
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(
        UnauthorizedException ex, WebRequest request) {

        ErrorResponse response = new ErrorResponse(
            LocalDateTime.now().toString(),
            HttpStatus.UNAUTHORIZED.value(),
            "Unauthorized",
            ex.getErrorCode(),
            ex.getMessage(),
            request.getDescription(false).replace("uri=", ""),
            null
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
        AccessDeniedException ex, WebRequest request) {

        ErrorResponse response = new ErrorResponse(
            LocalDateTime.now().toString(),
            HttpStatus.FORBIDDEN.value(),
            "Access Denied",
            "ACCESS_DENIED",
            ex.getMessage(),
            request.getDescription(false).replace("uri=", ""),
            null
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
        ValidationException ex, WebRequest request) {

        ErrorResponse response = new ErrorResponse(
            LocalDateTime.now().toString(),
            HttpStatus.BAD_REQUEST.value(),
            "Validation Error",
            ex.getErrorCode(),
            ex.getMessage(),
            request.getDescription(false).replace("uri=", ""),
            null
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex, WebRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error ->
            fieldErrors.put(((FieldError) error).getField(), error.getDefaultMessage())
        );

        ErrorResponse response = new ErrorResponse(
            LocalDateTime.now().toString(),
            HttpStatus.BAD_REQUEST.value(),
            "Validation Error",
            "VALIDATION_ERROR",
            "Input validation failed",
            request.getDescription(false).replace("uri=", ""),
            fieldErrors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
        HttpMessageNotReadableException ex, WebRequest request) {

        ErrorResponse response = new ErrorResponse(
            LocalDateTime.now().toString(),
            HttpStatus.BAD_REQUEST.value(),
            "Validation Error",
            "VALIDATION_ERROR",
            "Malformed request body",
            request.getDescription(false).replace("uri=", ""),
            null
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
        MethodArgumentTypeMismatchException ex, WebRequest request) {

        ErrorResponse response = new ErrorResponse(
            LocalDateTime.now().toString(),
            HttpStatus.BAD_REQUEST.value(),
            "Validation Error",
            "VALIDATION_ERROR",
            "Invalid request parameter type",
            request.getDescription(false).replace("uri=", ""),
            null
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(BookingConflictException.class)
    public ResponseEntity<ErrorResponse> handleBookingConflict(
        BookingConflictException ex, WebRequest request) {

        ErrorResponse response = new ErrorResponse(
            LocalDateTime.now().toString(),
            HttpStatus.CONFLICT.value(),
            "Booking Conflict",
            ex.getErrorCode(),
            ex.getMessage(),
            request.getDescription(false).replace("uri=", ""),
            null
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockingFailure(
        OptimisticLockingFailureException ex, WebRequest request) {

        ErrorResponse response = new ErrorResponse(
            LocalDateTime.now().toString(),
            HttpStatus.CONFLICT.value(),
            "Concurrency Conflict",
            "CONCURRENCY_CONFLICT",
            "The resource was updated by another request. Please retry.",
            request.getDescription(false).replace("uri=", ""),
            null
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(PaymentFailedException.class)
    public ResponseEntity<ErrorResponse> handlePaymentFailed(
        PaymentFailedException ex, WebRequest request) {

        ErrorResponse response = new ErrorResponse(
            LocalDateTime.now().toString(),
            HttpStatus.PAYMENT_REQUIRED.value(),
            "Payment Failed",
            ex.getErrorCode(),
            ex.getMessage(),
            request.getDescription(false).replace("uri=", ""),
            null
        );

        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
        Exception ex, WebRequest request) {

        log.error("Unexpected exception", ex);

        ErrorResponse response = new ErrorResponse(
            LocalDateTime.now().toString(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            "INTERNAL_ERROR",
            "An unexpected error occurred. Please try again later.",
            request.getDescription(false).replace("uri=", ""),
            null
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
