package com.socialmedia.user.event.outgoing;

import java.time.Instant;
import java.util.UUID;

public record UserBlockedEvent(UUID blockerId, UUID blockedId, Instant occurredAt) {
}
