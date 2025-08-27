package org.example.userservice.dto;

import org.example.userservice.enums.Role;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String firstname,
        String firstName,
        String email,
        String password,
        Role role
) {
}
