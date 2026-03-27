package com.grace.platform.shared.infrastructure.config;

import com.grace.platform.shared.application.dto.ApiResponse;
import com.grace.platform.shared.ErrorCode;
import com.grace.platform.shared.infrastructure.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handle(EntityNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.getErrorCode(), e.getMessage()));
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handle(BusinessRuleViolationException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getErrorCode(), e.getMessage()));
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handle(ExternalServiceException e) {
        HttpStatus status = resolveHttpStatus(e.getErrorCode());
        return ResponseEntity.status(status)
                .body(ApiResponse.error(e.getErrorCode(), e.getMessage()));
    }

    @ExceptionHandler(EncryptionException.class)
    public ResponseEntity<ApiResponse<Void>> handle(EncryptionException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(e.getErrorCode(), "Internal encryption error"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handle(MethodArgumentNotValidException e) {
        var fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .map(f -> new ApiResponse.FieldError(f.getField(), f.getDefaultMessage()))
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.INVALID_METADATA, "Validation failed", fieldErrors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception e) {
        // 记录日志
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR, "Internal server error"));
    }

    private HttpStatus resolveHttpStatus(int errorCode) {
        return switch (errorCode) {
            case ErrorCode.LLM_SERVICE_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case ErrorCode.OPENCRAWL_EXECUTION_FAILED, ErrorCode.PLATFORM_API_ERROR -> HttpStatus.BAD_GATEWAY;
            case ErrorCode.PLATFORM_QUOTA_EXCEEDED -> HttpStatus.TOO_MANY_REQUESTS;
            case ErrorCode.PLATFORM_AUTH_EXPIRED -> HttpStatus.UNAUTHORIZED;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
