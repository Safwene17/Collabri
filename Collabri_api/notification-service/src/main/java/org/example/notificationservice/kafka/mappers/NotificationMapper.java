package org.example.notificationservice.kafka.mappers;

import org.example.notificationservice.kafka.dtos.NotificationRequest;
import org.example.notificationservice.kafka.dtos.NotificationResponse;
import org.example.notificationservice.kafka.entities.Notification;
import org.example.notificationservice.kafka.enums.NotificationStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class NotificationMapper {

    public Notification toEntity(NotificationRequest request) {
        return Notification.builder()
                .title(request.title())
                .userId(request.userId())
                .message(request.message())
                .payload(request.payload())
                .type(request.type())
                .status(NotificationStatus.DELIVERED)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getUserId(),
                notification.getMessage(),
                notification.getPayload(),
                notification.getType() != null ? notification.getType().name() : null,
                notification.getStatus() != null ? notification.getStatus().name() : null,
                notification.getCreatedAt(),
                notification.getReadAt()
        );
    }

    public void updateFromRequest(NotificationRequest request, Notification notification) {
        notification.setTitle(request.title());
        notification.setUserId(request.userId());
        notification.setMessage(request.message());
        notification.setPayload(request.payload());
        notification.setType(request.type());
    }
}

