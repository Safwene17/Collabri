package org.example.calendarservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.calendarservice.entites.CalendarInvite;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import static org.springframework.messaging.support.MessageBuilder.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class InviteProducer {

    private final KafkaTemplate<String, CalendarInviteEvent> kafkaTemplate;

    public void sendCalendarInvitation(CalendarInviteEvent invite) {
        log.info("Sending CalendarInvitation for {}", invite);
        Message<CalendarInviteEvent> message = MessageBuilder
                .withPayload(invite)
                .setHeader(KafkaHeaders.TOPIC, "invite-topic")
                .build();
        kafkaTemplate.send(message);
    }
}
