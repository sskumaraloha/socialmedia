package com.socialmedia.search.event.incoming;

import java.time.Instant;
import java.util.UUID;

/** Local mirror of user-service's outgoing event.UserProfileCreatedEvent. */
public record UserProfileCreatedEvent(UUID userId, String username, Instant occurredAt) {
}
