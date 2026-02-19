package org.example.calendarservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record EventRequest(
        @NotBlank(message = "Title cannot be empty")
        String title,

        String description,

        @NotNull(message = "Start time cannot be null")
        LocalDateTime startTime,

        @NotNull(message = "End time cannot be null")
        LocalDateTime endTime,

        String location
) {
}