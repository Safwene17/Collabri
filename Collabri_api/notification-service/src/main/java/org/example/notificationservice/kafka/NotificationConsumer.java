package org.example.notificationservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.notificationservice.dtos.NotificationResponse;
import org.example.notificationservice.email.EmailService;
import org.example.notificationservice.kafka.calendar.CalendarInviteEvent;
import org.example.notificationservice.kafka.calendar.EventCreatedEvent;
import org.example.notificationservice.kafka.calendar.MemberJoinedEvent;
import org.example.notificationservice.kafka.calendar.MemberLeftEvent;
import org.example.notificationservice.entities.Notification;
import org.example.notificationservice.enums.NotificationStatus;
import org.example.notificationservice.enums.NotificationType;
import org.example.notificationservice.mappers.NotificationMapper;
import org.example.notificationservice.repositories.NotificationRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationMapper notificationMapper;

    @KafkaListener(topics = "calendar-invite-topic")
    public void consumeCalendarInvite(CalendarInviteEvent event) {
        log.info("Consumed invite message: {}", event);

        // Save notification to the database
        Notification notification = repository.save(
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

        // Push notification via WebSocket to connected clients
        pushNotificationToUser(notification);

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

        // Fan-out: Create and save one notification per userId, then push via WebSocket
        for (UUID recipientId : event.recipientsId()) {
            Notification notification = repository.save(
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

            // Push notification via WebSocket to the recipient
            pushNotificationToUser(notification);
        }
        log.info("Event created notifications saved and pushed for eventId={}", event.eventId());
    }

    @KafkaListener(topics = "calendar-task-topic")
    public void consumeTaskCreatedNotification(org.example.notificationservice.kafka.calendar.TaskCreatedEvent event) {
        log.info("Consumed task created message: {}", event);

        // Save notification to the database
        Notification notification = repository.save(
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

        // Push notification via WebSocket to the recipient
        pushNotificationToUser(notification);
    }

    @KafkaListener(topics = "calendar-member-joined-topic")
    public void consumeMemberJoinedNotification(MemberJoinedEvent event) {
        log.info("Consumed member joined message: {}", event);

        // Save notification to the database for each recipient and push via WebSocket
        for (UUID recipientId : event.recipientsId()) {
            Notification notification = repository.save(
                    Notification.builder()
                            .title(event.username() + " has joined " + event.calendarName())
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

            // Push notification via WebSocket to the recipient
            pushNotificationToUser(notification);
        }
    }

    @KafkaListener(topics = "calendar-member-left-topic")
    public void consumeMemberLeftNotification(MemberLeftEvent event) {
        log.info("Consumed member left message: {}", event);

        // Save notification to the database for each recipient and push via WebSocket
        for (UUID recipientId : event.recipientsId()) {
            Notification notification = repository.save(
                    Notification.builder()
                            .title(event.username() + " has left " + event.calendarName())
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

            // Push notification via WebSocket to the recipient
            pushNotificationToUser(notification);
        }
    }

    /**
     * Helper method to push notification to user via WebSocket.
     * Sends to user-specific destination: /user/{userId}/queue/notifications
     *
     * Only connected clients subscribed to their queue will receive the message.
     * If user is not connected, notification remains in database for later retrieval.
     */
    private void pushNotificationToUser(Notification notification) {
        try {
            String userId = notification.getUserId().toString();
            NotificationResponse response = notificationMapper.toResponse(notification);

            // Send to user-specific destination
            // Spring's SimpMessagingTemplate handles the /user/{userId} routing
            messagingTemplate.convertAndSendToUser(
                    userId,
                    "/queue/notifications",
                    response
            );

            log.debug("Pushed notification via WebSocket - userId: {}, notificationId: {}",
                     userId, notification.getId());
        } catch (Exception e) {
            // Log error but don't fail the Kafka consumer
            // Notification is already persisted in database
            log.error("Failed to push notification via WebSocket - notificationId: {}, error: {}",
                     notification.getId(), e.getMessage(), e);
        }
    }
}

