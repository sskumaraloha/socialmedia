package com.socialmedia.notification.dto.response;

import com.socialmedia.notification.domain.NotificationChannel;
import com.socialmedia.notification.domain.NotificationStatus;
import java.time.Instant;
import java.util.UUID;

public record NotificationLogResponse(
        UUID id,
        NotificationChannel channel,
        String templateKey,
        NotificationStatus status,
        String errorMessage,
        Instant createdAt
) {
}
