package org.example.userservice.dto;

import java.util.UUID;

public record AdminResponse(
        UUID id,
        String name,
        String email,
        String password
) {
}
