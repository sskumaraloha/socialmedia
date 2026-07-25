package com.socialmedia.common.audit;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * A single, reusable way for any service to emit a structured audit-trail entry, instead of
 * every service inventing its own bespoke audit log table. Not auto-wired (same convention as
 * {@link com.socialmedia.common.websocket.RedisWebSocketRelay}): a service registers this as a
 * bean itself, since it needs that service's own {@code KafkaTemplate}. Consumed by
 * analytics-service into a generic, queryable audit trail. auth-service's own bespoke
 * {@code AuditLog} (password reset / 2FA / device-revoke events, with richer per-auth-domain
 * fields) predates this and is intentionally left as-is rather than migrated.
 */
public class AuditEventPublisher {

    public static final String TOPIC = "audit.event.v1";

    private static final Logger log = LoggerFactory.getLogger(AuditEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public AuditEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(UUID actorUserId, String action, String targetType, String targetId, Map<String, String> metadata) {
        AuditEvent event = new AuditEvent(actorUserId, action, targetType, targetId,
                metadata == null ? Map.of() : metadata, Instant.now());
        try {
            kafkaTemplate.send(TOPIC, targetId, event).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.warn("Failed to publish audit event {} for {} {}: {}", action, targetType, targetId, ex.getMessage());
                }
            });
        } catch (Exception ex) {
            log.warn("Failed to publish audit event {} for {} {}: {}", action, targetType, targetId, ex.getMessage());
        }
    }
}
