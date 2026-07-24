package com.socialmedia.auth.event;

import java.time.Instant;
import java.util.UUID;

public record TwoFactorChangedEvent(UUID userId, boolean enabled, Instant occurredAt) {
}
