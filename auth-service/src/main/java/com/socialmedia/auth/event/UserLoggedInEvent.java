package com.socialmedia.auth.event;

import java.time.Instant;
import java.util.UUID;

public record UserLoggedInEvent(UUID userId, String deviceId, String ipAddress, Instant occurredAt) {
}
