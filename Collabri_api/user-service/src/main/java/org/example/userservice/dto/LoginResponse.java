package org.example.userservice.dto;

public record LoginResponse(
        String access_token,
        String refresh_token
) {
}
