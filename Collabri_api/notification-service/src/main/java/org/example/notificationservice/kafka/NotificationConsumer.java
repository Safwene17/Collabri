package org.example.notificationservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.notificationservice.kafka.calendar.CalendarInviteEvent;
import org.example.notificationservice.kafka.email.EmailService;
import org.example.notificationservice.kafka.entities.Notification;
import org.example.notificationservice.kafka.enums.NotificationType;
import org.example.notificationservice.kafka.repositories.NotificationRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final EmailService emailService;
    private final NotificationRepository repository;

    @KafkaListener(topics = "invite-topic")
    public void consumeCalendarInvite(CalendarInviteEvent event) {
        log.info("Consumed invite message: {}", event);


        // Save notification to the database
        repository.save(
                Notification.builder()
                        .type(NotificationType.CALENDAR_INVITATION)
                        .notificationDate(LocalDateTime.now())
                        .content(event)
                        .build()
        );

        try {
            // event fields: calendarId, calendarName, inviterEmail, destinationEmail, token, expiresAt
            emailService.sendCalendarInvitationEmail(
                    event.calendarId(),
                    event.calendarName(),
                    event.destinationEmail(),   // destination (recipient)
                    event.inviterEmail(),       // inviter (sender shown in body)
                    event.token(),
                    event.expiresAt()
            );
        } catch (Exception e) {
            log.error("Failed to handle CalendarInviteEvent (calendarId={}): {}", event.calendarId(), e.getMessage(), e);
            // Consider publishing to a dead-letter topic here for manual inspection / retry
        }
    }
}
