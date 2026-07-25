package com.socialmedia.media.event;

import com.socialmedia.media.domain.MediaKind;
import java.time.Instant;
import java.util.UUID;

public record MediaUploadedEvent(UUID mediaId, UUID ownerId, String originalFilename, MediaKind kind, Instant occurredAt) {
}
