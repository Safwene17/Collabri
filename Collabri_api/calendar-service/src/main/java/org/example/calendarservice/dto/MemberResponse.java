package org.example.calendarservice.dto;

import org.example.calendarservice.enums.Role;

import java.util.UUID;

public record MemberResponse(
        Long id,
        UUID userId,
        String displayName,
        String email,
        Role role
) {
}
