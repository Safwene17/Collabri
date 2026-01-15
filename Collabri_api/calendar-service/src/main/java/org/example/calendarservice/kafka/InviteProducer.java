package org.example.calendarservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class InviteProducer {

    private final KafkaTemplate<String, CalendarInviteEvent> kafkaTemplate;

    public void sendCalendarInvitation(CalendarInviteEvent invite) {
        log.info("Sending CalendarInvitation for {}", invite);
        Message<CalendarInviteEvent> message = MessageBuilder
                .withPayload(invite)
                .setHeader(KafkaHeaders.TOPIC, "calendar-invite-topic" )
                .build();
        kafkaTemplate.send(message);
    }

    public void sendEventCreatedNotification(EventCreatedEvent event) {
        log.info("Sending EventCreatedNotification for {}", event);
        Message<EventCreatedEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, "calendar-event-topic" )
                .build();
        kafkaTemplate.send(message);
    }

    public void sendTaskCreatedNotification(TaskCreatedEvent task) {
        log.info("Sending TaskCreatedNotification for {}", task);
        Message<TaskCreatedEvent> message = MessageBuilder
                .withPayload(task)
                .setHeader(KafkaHeaders.TOPIC, "calendar-task-topic" )
                .build();
        kafkaTemplate.send(message);
    }

    public void sendMemberJoinedNotification(MemberJoinedEvent memberActivityEvent) {
        log.info("Sending MemberJoinedNotification for {}", memberActivityEvent);
        Message<MemberJoinedEvent> message = MessageBuilder
                .withPayload(memberActivityEvent)
                .setHeader(KafkaHeaders.TOPIC, "calendar-member-joined-topic" )
                .build();
        kafkaTemplate.send(message);
    }

    public void sendMemberLeftNotification(MemberLeftEvent memberLeftEvent) {
        log.info("Sending MemberLeftNotification for {}", memberLeftEvent);
        Message<MemberLeftEvent> message = MessageBuilder
                .withPayload(memberLeftEvent)
                .setHeader(KafkaHeaders.TOPIC, "calendar-member-left-topic" )
                .build();
        kafkaTemplate.send(message);
    }
}
