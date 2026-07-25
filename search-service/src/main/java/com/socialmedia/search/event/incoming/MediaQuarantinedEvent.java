package com.socialmedia.search.event.incoming;

import java.time.Instant;
import java.util.UUID;

/** Local mirror of media-service's outgoing event.MediaQuarantinedEvent - quarantined content must not stay searchable. */
public record MediaQuarantinedEvent(UUID mediaId, UUID ownerId, Instant occurredAt) {
}
