package com.socialmedia.search.event.incoming;

import java.time.Instant;
import java.util.UUID;

/** Local mirror of user-service's outgoing event.UserProfileUpdatedEvent. */
public record UserProfileUpdatedEvent(UUID userId, String username, String displayName, String bio,
        String avatarUrl, Instant occurredAt) {
}
