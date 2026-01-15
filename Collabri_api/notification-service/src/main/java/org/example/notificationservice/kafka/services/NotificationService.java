package org.example.notificationservice.kafka.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.notificationservice.kafka.dtos.NotificationRequest;
import org.example.notificationservice.kafka.dtos.NotificationResponse;
import org.example.notificationservice.kafka.entities.Notification;
import org.example.notificationservice.kafka.enums.NotificationStatus;
import org.example.notificationservice.kafka.exceptions.NotificationNotFoundException;
import org.example.notificationservice.kafka.exceptions.NotificationValidationException;
import org.example.notificationservice.kafka.exceptions.UnauthorizedException;
import org.example.notificationservice.kafka.mappers.NotificationMapper;
import org.example.notificationservice.kafka.repositories.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    // ============ Ownership Check Helper ============

    /**
     * Verify that the notification belongs to the authenticated user
     */
    private void checkOwnership(String notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found with id: " + notificationId));

        if (!notification.getUserId().equals(userId)) {
            log.warn("Unauthorized access attempt for notification {} by user {}", notificationId, userId);
            throw new UnauthorizedException("You do not have permission to access this notification");
        }
    }

    // ============ Read Operations (User-Scoped) ============

    /**
     * Get all notifications for the authenticated user with pagination
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(UUID userId, Pageable pageable) {
        log.debug("Fetching notifications for authenticated user: {}", userId);

        if (userId == null) {
            throw new NotificationValidationException("User ID cannot be empty");
        }

        return notificationRepository.findByUserId(userId, pageable)
                .map(notificationMapper::toResponse);
    }

    /**
     * Get notification by ID (ownership verified)
     */
    @Transactional(readOnly = true)
    public NotificationResponse getMyNotification(String id, UUID userId) {
        log.debug("Fetching notification with id: {} for user: {}", id, userId);

        if (userId == null) {
            throw new NotificationValidationException("User ID cannot be empty");
        }

        checkOwnership(id, userId);

        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found with id: " + id));

        return notificationMapper.toResponse(notification);
    }

    /**
     * Get unread notifications for the authenticated user
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyUnreadNotifications(UUID userId, Pageable pageable) {
        log.debug("Fetching unread notifications for user: {}", userId);

        if (userId == null) {
            throw new NotificationValidationException("User ID cannot be empty");
        }

        return notificationRepository.findByUserIdAndStatus(userId, NotificationStatus.DELIVERED, pageable)
                .map(notificationMapper::toResponse);
    }

    /**
     * Get notifications by status for the authenticated user
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotificationsByStatus(UUID userId, NotificationStatus status, Pageable pageable) {
        log.debug("Fetching notifications for user: {} with status: {}", userId, status);

        if (userId == null) {
            throw new NotificationValidationException("User ID cannot be empty");
        }

        return notificationRepository.findByUserIdAndStatus(userId, status, pageable)
                .map(notificationMapper::toResponse);
    }

    /**
     * Get unread count for the authenticated user
     */
    @Transactional(readOnly = true)
    public long getMyUnreadCount(UUID userId) {
        log.debug("Getting unread count for user: {}", userId);

        if (userId == null) {
            throw new NotificationValidationException("User ID cannot be empty");
        }

        long count = notificationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                userId,
                NotificationStatus.DELIVERED
        ).size();

        log.debug("Unread count for {}: {}", userId, count);
        return count;
    }

    // ============ Write Operations (User-Scoped) ============

    /**
     * Mark notification as read (ownership verified)
     */
    @Transactional
    public NotificationResponse markMyNotificationAsRead(String id, UUID userId) {
        log.debug("Marking notification as read with id: {} for user: {}", id, userId);

        if (userId == null) {
            throw new NotificationValidationException("User ID cannot be empty");
        }

        checkOwnership(id, userId);

        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found with id: " + id));

        notification.setStatus(NotificationStatus.READ);
        notification.setReadAt(LocalDateTime.now());
        Notification updatedNotification = notificationRepository.save(notification);

        log.info("Notification marked as read with id: {} for user: {}", id, userId);
        return notificationMapper.toResponse(updatedNotification);
    }

    /**
     * Mark all unread notifications as read for the authenticated user
     */
    @Transactional
    public void markAllMyNotificationsAsRead(UUID userId) {
        log.debug("Marking all notifications as read for user: {}", userId);

        if (userId == null) {
            throw new NotificationValidationException("User ID cannot be empty");
        }

        List<Notification> unreadNotifications = notificationRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(userId, NotificationStatus.DELIVERED);

        LocalDateTime now = LocalDateTime.now();
        unreadNotifications.forEach(notification -> {
            notification.setStatus(NotificationStatus.READ);
            notification.setReadAt(now);
        });

        notificationRepository.saveAll(unreadNotifications);
        log.info("Marked {} notifications as read for user: {}", unreadNotifications.size(), userId);
    }

    /**
     * Delete notification (ownership verified)
     */
    @Transactional
    public void deleteMyNotification(String id, UUID userId) {
        log.debug("Deleting notification with id: {} for user: {}", id, userId);

        if (userId == null) {
            throw new NotificationValidationException("User ID cannot be empty");
        }

        checkOwnership(id, userId);

        if (!notificationRepository.existsById(id)) {
            throw new NotificationNotFoundException("Notification not found with id: " + id);
        }

        notificationRepository.deleteById(id);
        log.info("Notification deleted with id: {} for user: {}", id, userId);
    }

    /**
     * Delete all notifications for the authenticated user
     */
    @Transactional
    public void deleteAllMyNotifications(UUID userId) {
        log.debug("Deleting all notifications for user: {}", userId);

        if (userId == null) {
            throw new NotificationValidationException("User ID cannot be empty");
        }

        List<Notification> notifications = notificationRepository.findByUserId(userId, PageRequest.of(0, Integer.MAX_VALUE))
                .getContent();

        notificationRepository.deleteAll(notifications);
        log.info("Deleted {} notifications for user: {}", notifications.size(), userId);
    }

    // ============ Legacy Methods (For Internal Use / Kafka Consumer - DEPRECATED) ============

    /**
     * @deprecated Use user-scoped methods instead
     */
    @Deprecated
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getAllNotifications(Pageable pageable) {
        log.debug("Fetching all notifications (DEPRECATED)");
        return notificationRepository.findAll(pageable)
                .map(notificationMapper::toResponse);
    }

    /**
     * @deprecated Use getMyNotification instead
     */
    @Deprecated
    @Transactional(readOnly = true)
    public NotificationResponse getNotificationById(String id) {
        log.debug("Fetching notification with id: {} (DEPRECATED)", id);
        return notificationRepository.findById(id)
                .map(notificationMapper::toResponse)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found with id: " + id));
    }

    /**
     * @deprecated Use getMyNotifications instead
     */
    @Deprecated
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotificationsByRecipient(UUID userId, Pageable pageable) {
        return getMyNotifications(userId, pageable);
    }

    /**
     * @deprecated Use getMyUnreadNotifications instead
     */
    @Deprecated
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUnreadNotifications(UUID userId, Pageable pageable) {
        return getMyUnreadNotifications(userId, pageable);
    }

    /**
     * @deprecated Use getMyNotificationsByStatus instead
     */
    @Deprecated
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotificationsByRecipientAndStatus(
            UUID userId,
            NotificationStatus status,
            Pageable pageable) {
        return getMyNotificationsByStatus(userId, status, pageable);
    }

    /**
     * @deprecated Use updateMyNotification instead
     */
    @Deprecated
    @Transactional
    public NotificationResponse updateNotification(String id, NotificationRequest request) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found with id: " + id));
        notificationMapper.updateFromRequest(request, notification);
        return notificationMapper.toResponse(notificationRepository.save(notification));
    }

    /**
     * @deprecated Use markMyNotificationAsRead instead
     */
    @Deprecated
    @Transactional
    public NotificationResponse markAsRead(String id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found with id: " + id));
        notification.setStatus(NotificationStatus.READ);
        notification.setReadAt(LocalDateTime.now());
        return notificationMapper.toResponse(notificationRepository.save(notification));
    }

    /**
     * @deprecated Use markAllMyNotificationsAsRead instead
     */
    @Deprecated
    @Transactional
    public void markAllAsReadForRecipient(UUID userId) {
        markAllMyNotificationsAsRead(userId);
    }

    /**
     * @deprecated Use deleteMyNotification instead
     */
    @Deprecated
    @Transactional
    public void deleteNotification(String id) {
        if (!notificationRepository.existsById(id)) {
            throw new NotificationNotFoundException("Notification not found with id: " + id);
        }
        notificationRepository.deleteById(id);
    }

    /**
     * @deprecated Use deleteAllMyNotifications instead
     */
    @Deprecated
    @Transactional
    public void deleteAllNotificationsForRecipient(UUID userId) {
        deleteAllMyNotifications(userId);
    }

    /**
     * @deprecated Use getMyUnreadCount instead
     */
    @Deprecated
    @Transactional(readOnly = true)
    public long getUnreadCountForRecipient(UUID userId) {
        return getMyUnreadCount(userId);
    }
}

