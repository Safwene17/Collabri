package org.example.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "Firstname is required")
        @Size(min = 2, max = 30, message = "Firstname must be between 2 and 30 characters")
        String firstname,

        @NotBlank(message = "Lastname is required")
        @Size(min = 2, max = 30, message = "Lastname must be between 2 and 30 characters")
        String lastname,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email")
        String email,


        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @Pattern(regexp = "^(?=.*[A-Z])(?=.*[^a-zA-Z0-9]).+$",
                message = "Password must contain at least 1 uppercase letter and 1 special character")
        String password
) {
}

