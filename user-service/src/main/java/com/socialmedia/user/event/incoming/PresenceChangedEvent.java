package com.socialmedia.user.event.incoming;

import java.time.Instant;
import java.util.UUID;

/**
 * Local mirror of the contract presence-service's `presence.changed.v1` topic is
 * expected to publish (see docs/ARCHITECTURE.md event flow) - presence-service isn't
 * built yet, so this consumer is forward-looking: it registers the contract and no-ops
 * safely (Kafka consumers idle harmlessly against a topic that doesn't exist yet) until
 * that producer exists.
 */
public record PresenceChangedEvent(UUID userId, boolean online, Instant lastSeenAt) {
}
