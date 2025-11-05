package org.example.userservice.dto;

public record ResetPasswordRequest(String token, String newPassword) {
}
