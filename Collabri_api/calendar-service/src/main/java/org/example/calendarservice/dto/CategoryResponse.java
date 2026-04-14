package org.example.calendarservice.dto;

import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        String description,
        long calendarsCount
) {
}
