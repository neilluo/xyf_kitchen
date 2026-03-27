package com.grace.platform.shared.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
    int code,
    String message,
    T data,
    List<FieldError> errors,
    LocalDateTime timestamp
) {
    public record FieldError(String field, String message) {}

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "success", data, null, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(0, "success", null, null, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null, null, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> error(int code, String message, List<FieldError> errors) {
        return new ApiResponse<>(code, message, null, errors, LocalDateTime.now());
    }
}
