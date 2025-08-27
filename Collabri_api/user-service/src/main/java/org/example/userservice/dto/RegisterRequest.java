package org.example.userservice.dto;

import jakarta.validation.constraints.NotNull;

public record RegisterRequest(
        @NotNull(message = "Firstname cannot be null")
        String firstname,
         @NotNull(message = "Lastname cannot be null")
         String lastname,
            @NotNull(message = "Email cannot be null")
         String email,
            @NotNull(message = "Password cannot be null")
         String password

) {}

