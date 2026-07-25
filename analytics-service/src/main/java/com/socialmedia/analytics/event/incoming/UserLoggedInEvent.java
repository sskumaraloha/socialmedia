package com.socialmedia.analytics.event.incoming;

import java.time.Instant;
import java.util.UUID;

/** Must match auth-service's event.UserLoggedInEvent field-for-field. */
public record UserLoggedInEvent(UUID userId, String deviceId, String ipAddress, Instant occurredAt) {
}
