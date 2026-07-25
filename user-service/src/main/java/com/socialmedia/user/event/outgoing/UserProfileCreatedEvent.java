package com.socialmedia.user.event.outgoing;

import java.time.Instant;
import java.util.UUID;

public record UserProfileCreatedEvent(UUID userId, String username, Instant occurredAt) {
}
