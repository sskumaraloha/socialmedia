package com.socialmedia.presence.event;

import java.time.Instant;
import java.util.UUID;

/** Must match user-service's event.incoming.PresenceChangedEvent field-for-field. */
public record PresenceChangedEvent(UUID userId, boolean online, Instant lastSeenAt) {
}
