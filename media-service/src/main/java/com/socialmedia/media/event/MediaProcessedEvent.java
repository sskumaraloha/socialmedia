package com.socialmedia.media.event;

import java.time.Instant;
import java.util.UUID;

public record MediaProcessedEvent(UUID mediaId, UUID ownerId, boolean hasThumbnail, boolean transcoded, Instant occurredAt) {
}
