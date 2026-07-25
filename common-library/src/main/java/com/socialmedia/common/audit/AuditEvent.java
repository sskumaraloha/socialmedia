package com.socialmedia.common.audit;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Wire shape for every audit event published to {@link AuditEventPublisher#TOPIC}, consumed
 * platform-wide by analytics-service's generic audit trail. {@code actorUserId} is nullable -
 * some audit-worthy actions (e.g. a system job) have no acting user. */
public record AuditEvent(UUID actorUserId, String action, String targetType, String targetId,
        Map<String, String> metadata, Instant occurredAt) {
}
