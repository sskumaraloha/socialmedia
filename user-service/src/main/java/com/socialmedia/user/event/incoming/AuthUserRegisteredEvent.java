package com.socialmedia.user.event.incoming;

import java.time.Instant;
import java.util.UUID;

/**
 * Local mirror of auth-service's UserRegisteredEvent (topic auth.user.registered.v1) -
 * services never share domain classes, only the wire contract, so this is intentionally
 * a separate, decoupled definition even though the shape matches today.
 */
public record AuthUserRegisteredEvent(UUID userId, String email, String provider, Instant occurredAt) {
}
