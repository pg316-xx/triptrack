package com.tripnext.demo.dto.notification;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.tripnext.demo.model.help.NotificationType;

public record NotificationResponse(
        UUID id,
        UUID tripId,
        NotificationType type,
        String title,
        String message,
        String actionUrl,
        Boolean isRead,
        OffsetDateTime createdAt
) {
}
