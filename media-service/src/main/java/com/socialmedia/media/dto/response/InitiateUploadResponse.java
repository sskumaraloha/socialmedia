package com.socialmedia.media.dto.response;

import java.util.UUID;

public record InitiateUploadResponse(UUID mediaId, String uploadId, int recommendedPartSizeBytes) {
}
