package org.example.calendarservice.kafka;

import java.util.List;
import java.util.UUID;

public record EventCreatedEvent(
        UUID eventId,
        String title,
        String createdBy,
        String calendarName,
        String location,
        List<String> recipientEmails
) {
}
