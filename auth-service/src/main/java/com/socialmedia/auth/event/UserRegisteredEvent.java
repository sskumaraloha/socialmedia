package com.socialmedia.auth.event;

import java.time.Instant;
import java.util.UUID;

public record UserRegisteredEvent(UUID userId, String email, String provider, Instant occurredAt) {
}
