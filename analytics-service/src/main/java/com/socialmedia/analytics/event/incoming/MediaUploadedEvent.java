package com.socialmedia.analytics.event.incoming;

import java.time.Instant;
import java.util.UUID;

/** Must match media-service's event.MediaUploadedEvent field-for-field (kind kept as String here -
 * analytics-service has no need for media-service's MediaKind enum type). */
public record MediaUploadedEvent(UUID mediaId, UUID ownerId, String originalFilename, String kind, long sizeBytes,
        Instant occurredAt) {
}
