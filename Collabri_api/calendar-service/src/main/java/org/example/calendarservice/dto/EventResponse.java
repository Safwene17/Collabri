package org.example.calendarservice.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventResponse(
        UUID id,
        String title,
        String description,
        UUID calendarId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String location,
        LocalDateTime createdAt
) {
}