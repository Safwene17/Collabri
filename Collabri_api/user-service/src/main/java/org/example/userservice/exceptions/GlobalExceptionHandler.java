package org.example.userservice.exceptions;

import jakarta.validation.ConstraintViolationException;
import org.example.userservice.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Handle your own custom exception which carries an HttpStatus
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<?>> handleCustomException(CustomException ex) {
        HttpStatus status = ex.getStatus();
        if (status.is5xxServerError()) {
            // log stack trace for server errors
            log.error("CustomException -> {}", ex.getMessage(), ex);
        } else {
            log.warn("CustomException -> {}", ex.getMessage());
        }
        return ResponseEntity.status(status).body(ApiResponse.error(ex.getMessage()));
    }

    // Handle validation errors from @Valid (request body) - Override without @ExceptionHandler
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, List<String>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.groupingBy(
                        org.springframework.validation.FieldError::getField,
                        Collectors.mapping(org.springframework.validation.FieldError::getDefaultMessage, Collectors.toList())
                ));
        log.info("Validation errors: {}", fieldErrors);
        return ResponseEntity.status(status).body(ApiResponse.error("Validation failed", new HashMap<>(fieldErrors)));
    }

    // Handle JSON parse / bad request bodies - Override without @ExceptionHandler
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {

        log.info("Malformed JSON request: {}", ex.getMessage());
        return ResponseEntity.status(status).body(ApiResponse.error("Malformed request body"));
    }

    // Constraint violations (e.g. @Validated on method params)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleConstraintViolation(ConstraintViolationException ex) {
        String msg = ex.getConstraintViolations().stream()
                .findFirst()
                .map(cv -> cv.getMessage() != null ? cv.getMessage() : "Request parameter invalid")
                .orElse("Constraint violation");
        log.info("Constraint violation: {}", msg);
        return ResponseEntity.badRequest().body(ApiResponse.error(msg));
    }

    // Security: authentication failure (401)
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<?>> handleAuthenticationException(AuthenticationException ex) {
        log.info("Authentication failed: {}", ex.getMessage());
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).headers(headers).body(ApiResponse.error("Unauthorized"));
    }

    // Security: access denied (403)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Access denied"));
    }

    // Not found translations (NoSuchElementException often used)
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiResponse<?>> handleNotFound(NoSuchElementException ex) {
        log.info("Not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Resource not found"));
    }

    // Generic IllegalArgument -> 400
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<?>> handleIllegalArgument(IllegalArgumentException ex) {
        log.info("Bad request: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
    }

    // Fallback - don't leak internals; return safe generic error
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleAll(Exception ex) {
        // log stack trace on server side for diagnostics
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Internal server error"));
    }

}