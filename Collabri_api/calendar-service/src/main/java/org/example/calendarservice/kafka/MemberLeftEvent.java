package org.example.calendarservice.kafka;

import java.util.List;
import java.util.UUID;

public record MemberLeftEvent(
        String username,
        UUID calendarId,
        String calendarName,
        List<UUID> recipientsId
) {
}
