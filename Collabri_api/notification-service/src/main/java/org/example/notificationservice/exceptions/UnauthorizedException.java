package org.example.notificationservice.exceptions;

/**
 * Exception thrown when user attempts to access resources they don't own.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}

