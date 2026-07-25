package com.socialmedia.search.event.incoming;

import java.time.Instant;
import java.util.UUID;

/** Local mirror of media-service's outgoing event.MediaUploadedEvent. */
public record MediaUploadedEvent(UUID mediaId, UUID ownerId, String originalFilename, String kind, Instant occurredAt) {
}
