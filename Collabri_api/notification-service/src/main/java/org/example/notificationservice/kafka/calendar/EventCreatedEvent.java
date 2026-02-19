package org.example.notificationservice.kafka.calendar;

import java.util.List;
import java.util.UUID;

public record EventCreatedEvent(
        UUID eventId,
        String title,
        String createdBy,
        String calendarName,
        String location,
        List<UUID> recipientsId
) {
}
