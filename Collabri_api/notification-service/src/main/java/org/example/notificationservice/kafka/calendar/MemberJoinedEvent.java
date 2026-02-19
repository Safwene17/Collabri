package org.example.notificationservice.kafka.calendar;

import java.util.List;
import java.util.UUID;

public record MemberJoinedEvent(
        String username,
        UUID calendarId,
        String calendarName,
        List<UUID> recipientsId
) {
}
