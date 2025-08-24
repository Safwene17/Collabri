package org.example.userservice.dto;

import jakarta.validation.constraints.NotNull;

public record LoginRequest(
        @NotNull(message = "Email cannot be null")
        String email,
        @NotNull(message = "password cannot be null")
        String password
) {
}
