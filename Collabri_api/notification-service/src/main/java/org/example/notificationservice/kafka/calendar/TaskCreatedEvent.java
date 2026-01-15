package org.example.notificationservice.kafka.calendar;

import java.util.UUID;

public record TaskCreatedEvent(
        UUID taskId,
        String title,
        String assignTo,
        String createdBy,
        String calendarName,
        UUID recipientId
) {
}
