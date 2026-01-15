package org.example.notificationservice.kafka.enums;

public enum NotificationStatus {
    DELIVERED,
    READ,
    @Deprecated
    DELEIVERED // Misspelled version for backward compatibility with existing data
}
