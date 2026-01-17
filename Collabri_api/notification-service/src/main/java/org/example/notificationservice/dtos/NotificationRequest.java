package org.example.notificationservice.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.notificationservice.enums.NotificationType;

import java.util.Map;
import java.util.UUID;

public record NotificationRequest(
        @NotBlank(message = "Title cannot be blank")
        String title,

        @NotBlank(message = "userId cannot be blank")
        UUID userId,

        @NotBlank(message = "Message cannot be blank")
        String message,

        Map<String, Object> payload,

        @NotNull(message = "Notification type is required")
        NotificationType type
) {
}

