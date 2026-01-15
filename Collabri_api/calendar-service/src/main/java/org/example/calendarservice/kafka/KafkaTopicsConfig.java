package org.example.calendarservice.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicsConfig {

    @Bean
    public NewTopic InviteTopic() {
        return TopicBuilder
                .name("calendar-invite-topic" )
                .build();
    }

    @Bean
    public NewTopic EventTopic() {
        return TopicBuilder
                .name("calendar-event-topic" )
                .build();
    }

    @Bean
    public NewTopic TaskTopic() {
        return TopicBuilder
                .name("calendar-task-topic" )
                .build();
    }

    @Bean
    public NewTopic MemberJoinedTopic() {
        return TopicBuilder
                .name("calendar-member-joined-topic" )
                .build();
    }

    @Bean
    public NewTopic MemberLeftTopic() {
        return TopicBuilder
                .name("calendar-member-left-topic" )
                .build();
    }
}
