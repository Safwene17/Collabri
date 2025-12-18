// src/main/java/org/example/gateway/exceptions/GlobalExceptionHandler.java
package org.example.gateway.exceptions;

import io.jsonwebtoken.ExpiredJwtException;
import org.example.gateway.dto.ApiResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpServerErrorException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Validation errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");
        return ResponseEntity.badRequest().body(ApiResponse.error(message));
    }

    // 2. Service down / connection refused → 503
    @ExceptionHandler(HttpServerErrorException.ServiceUnavailable.class)
    public ResponseEntity<ApiResponse<?>> handleServiceUnavailable() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error("Service is currently unavailable. Please try again later."));
    }

    // 3. Any other 5xx from downstream (optional, but nice)
    @ExceptionHandler(HttpServerErrorException.class)
    public ResponseEntity<ApiResponse<?>> handleServerError(HttpServerErrorException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(ApiResponse.error("Service is currently unavailable. Please try again later."));
    }

    // 4. Expired JWT (from your filter)
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ApiResponse<?>> handleExpiredJwt() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.WWW_AUTHENTICATE,
                "Bearer error=\"invalid_token\", error_description=\"The access token expired\"");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .headers(headers)
                .body(ApiResponse.error("Token has expired"));
    }

    // 5. Fallback — never leak stack traces
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleAll(Exception ex) {
        // Log it properly in production
        // log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Internal server error"));
    }
}