package org.example.notificationservice.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record NotificationResponse(
        String id,
        String title,
        UUID userId,
        String message,
        Map<String, Object> payload,
        String type,
        String status,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {
}

