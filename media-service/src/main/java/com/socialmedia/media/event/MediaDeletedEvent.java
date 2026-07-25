package com.socialmedia.media.event;

import java.time.Instant;
import java.util.UUID;

public record MediaDeletedEvent(UUID mediaId, UUID ownerId, Instant occurredAt) {
}
