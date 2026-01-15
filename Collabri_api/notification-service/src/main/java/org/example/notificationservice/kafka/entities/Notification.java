package org.example.notificationservice.kafka.entities;

import lombok.*;
import org.example.notificationservice.kafka.enums.NotificationStatus;
import org.example.notificationservice.kafka.enums.NotificationType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Document(collection = "collabri-notifications")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class Notification {

    @Id
    private String id;
    private String title;
    private UUID userId;
    private String message;
    private Map<String, Object> payload;
    private NotificationType type;
    private NotificationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;

}
