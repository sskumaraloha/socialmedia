package com.socialmedia.notification.dto.request;

import com.socialmedia.notification.domain.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record SendNotificationRequest(
        @NotNull NotificationChannel channel,
        @NotBlank String templateKey,
        Map<String, String> templateParams,
        String locale,
        String recipientEmail,
        String recipientPhone
) {
}
