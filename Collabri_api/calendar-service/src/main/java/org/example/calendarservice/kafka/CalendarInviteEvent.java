package org.example.calendarservice.kafka;

import java.time.Instant;
import java.util.UUID;

public record CalendarInviteEvent(
        UUID calendarId,
        String calendarName,
        String inviterEmail,
        String destinationEmail,
        String token,          // plaintext token — only on internal topic
        Instant expiresAt
) {
}
