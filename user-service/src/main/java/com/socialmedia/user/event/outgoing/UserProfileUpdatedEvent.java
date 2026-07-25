package com.socialmedia.user.event.outgoing;

import java.time.Instant;
import java.util.UUID;

public record UserProfileUpdatedEvent(UUID userId, String username, String displayName, String bio,
        String avatarUrl, Instant occurredAt) {
}
