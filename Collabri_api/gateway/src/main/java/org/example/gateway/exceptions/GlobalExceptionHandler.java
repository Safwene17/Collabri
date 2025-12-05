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

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(message));
    }

    // Optional: handle unexpected errors
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleAll(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Unexpected error occurred"));
    }

    // This is only for cases where exception bubbles up (shouldn't happen with filter)
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ApiResponse<?>> handleExpiredJwt() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.WWW_AUTHENTICATE,
                "Bearer error=\"invalid_token\", error_description=\"The access token expired\"");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .headers(headers)
                .body(ApiResponse.error("Token has expired"));
    }
}