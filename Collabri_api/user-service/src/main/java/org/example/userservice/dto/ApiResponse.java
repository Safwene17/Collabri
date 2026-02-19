package org.example.userservice.dto;

import lombok.Builder;

import java.util.Map;

@Builder
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        Map<String, Object> errors  // New for details
) {
    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data, null);
    }

    public static <T> ApiResponse<T> error(String message, Map<String, Object> errors) {
        return new ApiResponse<>(false, message, null, errors);
    }

    public static <T> ApiResponse<T> error(String message) {
        return error(message, null);
    }
}