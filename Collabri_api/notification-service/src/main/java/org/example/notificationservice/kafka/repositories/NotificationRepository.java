package org.example.notificationservice.kafka.repositories;

import org.example.notificationservice.kafka.entities.Notification;
import org.example.notificationservice.kafka.enums.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {

    Page<Notification> findByUserId(UUID userId, Pageable pageable);

    Page<Notification> findByUserIdAndStatus(UUID userId, NotificationStatus status, Pageable pageable);

    List<Notification> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, NotificationStatus status);

}
