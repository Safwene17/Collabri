package org.example.notificationservice.enums;

public enum NotificationStatus {
    DELIVERED,
    READ,
    @Deprecated
    DELEIVERED // Misspelled version for backward compatibility with existing data
}
