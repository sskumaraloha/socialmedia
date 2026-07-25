package com.socialmedia.notification.event.incoming;

import java.time.Instant;
import java.util.UUID;

/** Local mirror of auth-service's outgoing event.PasswordResetRequestedEvent. */
public record PasswordResetRequestedEvent(UUID userId, String email, String resetToken, Instant occurredAt) {
}
