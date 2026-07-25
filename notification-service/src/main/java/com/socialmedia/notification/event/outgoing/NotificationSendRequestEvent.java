package com.socialmedia.notification.event.outgoing;

import com.socialmedia.notification.domain.NotificationChannel;
import java.util.Map;
import java.util.UUID;

public record NotificationSendRequestEvent(
        UUID userId,
        NotificationChannel channel,
        String templateKey,
        Map<String, String> templateParams,
        String locale,
        String recipientEmail,
        String recipientPhone
) {
}
