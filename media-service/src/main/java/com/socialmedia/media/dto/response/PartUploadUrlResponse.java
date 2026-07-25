package com.socialmedia.media.dto.response;

import java.time.Instant;

public record PartUploadUrlResponse(int partNumber, String url, Instant expiresAt) {
}
