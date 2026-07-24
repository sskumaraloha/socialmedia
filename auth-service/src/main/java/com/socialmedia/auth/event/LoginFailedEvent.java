package com.socialmedia.auth.event;

import java.time.Instant;

public record LoginFailedEvent(String email, String ipAddress, String reason, Instant occurredAt) {
}
