package com.socialmedia.auth.dto.response;

import java.time.Instant;
import java.util.UUID;

public record LoginHistoryResponse(
        UUID id,
        String ipAddress,
        String userAgent,
        boolean success,
        String failureReason,
        Instant createdAt
) {
}
