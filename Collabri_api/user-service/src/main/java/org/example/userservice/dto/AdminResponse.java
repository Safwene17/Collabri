package org.example.userservice.dto;

public record AdminResponse(
        String id,
        String name,
        String email,
        String password
) {
}
