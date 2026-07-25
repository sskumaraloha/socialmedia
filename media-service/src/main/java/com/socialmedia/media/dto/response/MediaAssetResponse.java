package com.socialmedia.media.dto.response;

import com.socialmedia.media.domain.MediaKind;
import com.socialmedia.media.domain.MediaStatus;
import com.socialmedia.media.domain.VirusScanStatus;
import java.time.Instant;
import java.util.UUID;

public record MediaAssetResponse(
        UUID id,
        UUID ownerId,
        String originalFilename,
        String mimeType,
        MediaKind kind,
        Long sizeBytes,
        MediaStatus status,
        VirusScanStatus virusScanStatus,
        boolean transcoded,
        String accessUrl,
        String thumbnailUrl,
        Instant createdAt
) {
}
