package org.example.calendarservice.user;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UserResponse(

        @NotNull(message = "id cannot be null")
        UUID id,

        @NotNull(message = "Firstname cannot be null")
        String firstname,

        @NotNull(message = "lastname  cannot be null")
        String lastname,

        @NotNull(message = "email cannot be null")
        String email
) {
}
