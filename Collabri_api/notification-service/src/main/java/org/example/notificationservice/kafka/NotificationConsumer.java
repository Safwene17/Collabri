package org.example.notificationservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.notificationservice.kafka.calendar.CalendarInviteEvent;
import org.example.notificationservice.kafka.calendar.EventCreatedEvent;
import org.example.notificationservice.kafka.calendar.MemberJoinedEvent;
import org.example.notificationservice.kafka.calendar.MemberLeftEvent;
import org.example.notificationservice.kafka.email.EmailService;
import org.example.notificationservice.kafka.entities.Notification;
import org.example.notificationservice.kafka.enums.NotificationStatus;
import org.example.notificationservice.kafka.enums.NotificationType;
import org.example.notificationservice.kafka.repositories.NotificationRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final EmailService emailService;
    private final NotificationRepository repository;

    @KafkaListener(topics = "calendar-invite-topic")
    public void consumeCalendarInvite(CalendarInviteEvent event) {
        log.info("Consumed invite message: {}", event);
        // Save notification to the database
        repository.save(
                Notification.builder()
                        .title("Calendar Invitation")
                        .userId(event.userId())
                        .message("You have been invited to a calendar.")
                        .payload(Map.of(
                                "calendarId", event.calendarId(),
                                "calendarName", event.calendarName(),
                                "inviterEmail", event.inviterEmail(),
                                "destinationEmail", event.destinationEmail()
                        ))
                        .type(NotificationType.CALENDAR_INVITATION)
                        .status(NotificationStatus.DELIVERED)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        try {
            // event fields: calendarId, calendarName, inviterEmail, destinationEmail, token, expiresAt
            emailService.sendCalendarInvitationEmail(
                    event.calendarId(),
                    event.calendarName(),
                    event.destinationEmail(),   // destination
                    event.inviterEmail(),       // inviter (sender shown in body)
                    event.token(),
                    event.expiresAt()
            );
        } catch (Exception e) {
            log.error("Failed to handle CalendarInviteEvent (calendarId={}): {}", event.calendarId(), e.getMessage(), e);
            // Consider publishing to a dead-letter topic here for manual inspection / retry
        }
    }

    @KafkaListener(topics = "calendar-event-topic")
    public void consumeEventCreatedNotification(EventCreatedEvent event) {
        log.info("Consumed event created message: {}", event);

        // Fan-out: Create and save one notification per userId email
        for (UUID recipientId : event.recipientsId()) {
            repository.save(
                    Notification.builder()
                            .userId(recipientId)
                            .title("Event created: " + event.title())
                            .message(event.title() + " in " + event.calendarName() + " — " + event.location())
                            .payload(Map.of(
                                    "eventId", event.eventId().toString(),
                                    "title", event.title(),
                                    "calendarName", event.calendarName(),
                                    "location", event.location()
                            ))
                            .type(NotificationType.EVENT_CREATED)
                            .status(NotificationStatus.DELIVERED)
                            .createdAt(LocalDateTime.now())
                            .build()
            );
        }
        log.info("Event created notifications saved for eventId={}", event.eventId());
    }

    @KafkaListener(topics = "calendar-task-topic")
    public void consumeTaskCreatedNotification(org.example.notificationservice.kafka.calendar.TaskCreatedEvent event) {
        log.info("Consumed task created message: {}", event);
        // Save notification to the database
        repository.save(
                Notification.builder()
                        .userId(event.recipientId())
                        .title("Task assigned: " + event.title())
                        .message("You have been assigned a new task in " + event.calendarName())
                        .payload(Map.of(
                                "taskId", event.taskId().toString(),
                                "title", event.title(),
                                "assignTo", event.assignTo(),
                                "createdBy", event.createdBy(),
                                "calendarName", event.calendarName()
                        ))
                        .type(NotificationType.TASK_CREATED)
                        .status(NotificationStatus.DELIVERED)
                        .createdAt(LocalDateTime.now())
                        .build()
        );
    }

    ;

    @KafkaListener(topics = "calendar-member-joined-topic")
    public void consumeMemberJoinedNotification(MemberJoinedEvent event) {
        log.info("Consumed member joined message: {}", event);
        // Save notification to the database
        for (UUID recipientId : event.recipientsId()) {
            repository.save(
                    Notification.builder()
                            .title(event.username() + "has joined " + event.calendarName())
                            .message("Check out the calendar now!")
                            .userId(recipientId)
                            .type(NotificationType.MEMBER_JOINED)
                            .payload(
                                    Map.of(
                                            "calendarId", event.calendarId().toString(),
                                            "calendarName", event.calendarName(),
                                            "username", event.username()
                                    )
                            )
                            .status(NotificationStatus.DELIVERED)
                            .createdAt(LocalDateTime.now())
                            .build()
            );
        }
    }

    @KafkaListener(topics = "calendar-member-left-topic")
    public void consumeMemberLeftNotification(MemberLeftEvent event) {
        log.info("Consumed member left message: {}", event);
        // Save notification to the database
        for (UUID recipientId : event.recipientsId()) {
            repository.save(
                    Notification.builder()
                            .title(event.username() + "has left " + event.calendarName())
                            .message("Check out the calendar now!")
                            .userId(recipientId)
                            .type(NotificationType.MEMBER_LEFT)
                            .payload(
                                    Map.of(
                                            "calendarId", event.calendarId().toString(),
                                            "calendarName", event.calendarName(),
                                            "username", event.username()
                                    )
                            )
                            .status(NotificationStatus.DELIVERED)
                            .createdAt(LocalDateTime.now())
                            .build()
            );
        }
    }

}

