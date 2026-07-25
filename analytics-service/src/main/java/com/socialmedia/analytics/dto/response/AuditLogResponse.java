package com.socialmedia.analytics.dto.response;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditLogResponse(UUID actorUserId, String action, String targetType, String targetId,
        Map<String, String> metadata, Instant occurredAt) {
}
