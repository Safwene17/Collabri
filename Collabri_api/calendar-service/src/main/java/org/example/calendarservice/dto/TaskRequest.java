package org.example.calendarservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.calendarservice.enums.TaskStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record TaskRequest(
        @NotBlank(message = "Title cannot be empty")
        String title,

        String description,

        @NotNull(message = "Assigned member cannot be null")
        UUID assignedTo,

        @NotNull(message = "Due date cannot be null")
        LocalDateTime dueDate,

        @NotNull(message = "Task status cannot be null")
        TaskStatus taskStatus
) {
}