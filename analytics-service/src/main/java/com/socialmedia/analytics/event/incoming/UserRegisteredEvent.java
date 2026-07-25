package com.socialmedia.analytics.event.incoming;

import java.time.Instant;
import java.util.UUID;

/** Must match auth-service's event.UserRegisteredEvent field-for-field. */
public record UserRegisteredEvent(UUID userId, String email, String provider, Instant occurredAt) {
}
