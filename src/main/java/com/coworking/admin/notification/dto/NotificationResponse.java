package com.coworking.admin.notification.dto;

import com.coworking.admin.notification.enums.NotificationType;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String message,
        String entityType,
        Long entityId,
        boolean read,
        Instant createdAt
) {
}
