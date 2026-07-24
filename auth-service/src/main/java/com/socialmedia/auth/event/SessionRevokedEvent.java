package com.socialmedia.auth.event;

import java.time.Instant;
import java.util.UUID;

public record SessionRevokedEvent(UUID userId, UUID sessionId, String deviceId, String reason, Instant occurredAt) {
}
