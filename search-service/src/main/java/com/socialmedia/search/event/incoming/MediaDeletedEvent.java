package com.socialmedia.search.event.incoming;

import java.time.Instant;
import java.util.UUID;

/** Local mirror of media-service's outgoing event.MediaDeletedEvent. */
public record MediaDeletedEvent(UUID mediaId, UUID ownerId, Instant occurredAt) {
}
