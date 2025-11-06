package org.example.userservice.exceptions;

public class InvalidPasswordFormatException extends RuntimeException {
    public InvalidPasswordFormatException() {
        super("Password must be at least 8 characters and include at least one number and one special character");
    }
}
