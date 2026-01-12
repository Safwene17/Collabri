package org.example.calendarservice.dto;

import org.example.calendarservice.enums.TaskStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        String title,
        String description,
        UUID calendarId,
        UUID assignedTo,
        LocalDateTime dueDate,
        TaskStatus status,
        LocalDateTime createdAt
) {
}