package org.example.notificationservice.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.notificationservice.dtos.ApiResponse;
import org.example.notificationservice.dtos.NotificationResponse;
import org.example.notificationservice.enums.NotificationStatus;
import org.example.notificationservice.services.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Secure Notification API endpoints.
 * All endpoints are user-scoped - userId is extracted from JWT authentication.
 * No path variables for userId to prevent unauthorized access.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Extract userId from JWT authentication principal
     */
    private UUID extractUserIdFromAuth(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid userId format in JWT: {}", authentication.getName());
            throw new IllegalArgumentException("Invalid user identifier in token");
        }
    }

    // ============ Read Endpoints ============

    /**
     * Get all notifications for authenticated user with pagination
     * GET /api/v1/notifications
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getMyNotifications(
            Authentication authentication,
            @PageableDefault(size = 20, page = 0) Pageable pageable) {
        log.info("Fetching notifications for authenticated user");

        UUID userId = extractUserIdFromAuth(authentication);
        Page<NotificationResponse> notifications = notificationService.getMyNotifications(userId, pageable);

        return ResponseEntity.ok(ApiResponse.success(notifications, "Your notifications retrieved successfully"));
    }

    /**
     * Get a specific notification by ID (ownership verified)
     * GET /api/v1/notifications/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NotificationResponse>> getMyNotification(
            Authentication authentication,
            @PathVariable String id) {
        log.info("Fetching notification with id: {}", id);

        UUID userId = extractUserIdFromAuth(authentication);
        NotificationResponse response = notificationService.getMyNotification(id, userId);

        return ResponseEntity.ok(ApiResponse.success(response, "Notification retrieved successfully"));
    }

    /**
     * Get unread notifications for authenticated user
     * GET /api/v1/notifications/unread
     */
    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getMyUnreadNotifications(
            Authentication authentication,
            @PageableDefault(size = 20, page = 0) Pageable pageable) {
        log.info("Fetching unread notifications for authenticated user");

        UUID userId = extractUserIdFromAuth(authentication);
        Page<NotificationResponse> notifications = notificationService.getMyUnreadNotifications(userId, pageable);

        return ResponseEntity.ok(ApiResponse.success(notifications, "Unread notifications retrieved successfully"));
    }

    /**
     * Get notifications by status for authenticated user
     * GET /api/v1/notifications/by-status?status=READ
     */
    @GetMapping("/by-status")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getMyNotificationsByStatus(
            Authentication authentication,
            @RequestParam NotificationStatus status,
            @PageableDefault(size = 20, page = 0) Pageable pageable) {
        log.info("Fetching notifications with status: {}", status);

        UUID userId = extractUserIdFromAuth(authentication);
        Page<NotificationResponse> notifications = notificationService.getMyNotificationsByStatus(userId, status, pageable);

        return ResponseEntity.ok(ApiResponse.success(notifications, "Notifications with status retrieved successfully"));
    }

    /**
     * Get unread notification count for authenticated user
     * GET /api/v1/notifications/unread-count
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getMyUnreadCount(Authentication authentication) {
        log.info("Getting unread notification count for authenticated user");

        UUID userId = extractUserIdFromAuth(authentication);
        long count = notificationService.getMyUnreadCount(userId);

        return ResponseEntity.ok(ApiResponse.success(count, "Unread count retrieved successfully"));
    }

    // ============ Write Endpoints ============

    /**
     * Mark a specific notification as read
     * PATCH /api/v1/notifications/{id}/mark-as-read
     */
    @PatchMapping("/{id}/mark-as-read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markMyNotificationAsRead(Authentication authentication, @PathVariable String id) {
        log.info("Marking notification as read with id: {}", id);
        UUID userId = extractUserIdFromAuth(authentication);
        NotificationResponse response = notificationService.markMyNotificationAsRead(id, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Notification marked as read successfully"));
    }

    /**
     * Mark all notifications as read for authenticated user
     * PATCH /api/v1/notifications/mark-all-as-read
     */
    @PatchMapping("/mark-all-as-read")
    public ResponseEntity<ApiResponse<Void>> markAllMyNotificationsAsRead(Authentication authentication) {
        log.info("Marking all notifications as read for authenticated user");

        UUID userId = extractUserIdFromAuth(authentication);
        notificationService.markAllMyNotificationsAsRead(userId);

        return ResponseEntity.ok(ApiResponse.success(null, "All notifications marked as read successfully"));
    }

    /**
     * Delete a specific notification
     * DELETE /api/v1/notifications/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMyNotification(
            Authentication authentication,
            @PathVariable String id) {
        log.info("Deleting notification with id: {}", id);

        UUID userId = extractUserIdFromAuth(authentication);
        notificationService.deleteMyNotification(id, userId);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResponse.success(null, "Notification deleted successfully"));
    }

    /**
     * Delete all notifications for authenticated user
     * DELETE /api/v1/notifications
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteAllMyNotifications(Authentication authentication) {
        log.info("Deleting all notifications for authenticated user");

        UUID userId = extractUserIdFromAuth(authentication);
        notificationService.deleteAllMyNotifications(userId);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResponse.success(null, "All notifications deleted successfully"));
    }
}

