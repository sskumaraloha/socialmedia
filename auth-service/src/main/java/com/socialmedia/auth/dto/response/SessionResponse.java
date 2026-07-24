package com.socialmedia.auth.dto.response;

import java.time.Instant;
import java.util.UUID;

public record SessionResponse(
        UUID id,
        String deviceId,
        String ipAddress,
        String userAgent,
        Instant issuedAt,
        Instant lastSeenAt,
        boolean current
) {
}
