package com.socialmedia.analytics.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * The platform-wide audit trail this module's README entry (see the Modules table -
 * "Usage analytics, audit trail") has always promised: every service publishes structured
 * audit events via common-library's AuditEventPublisher, and this is where they land.
 */
@Document(collection = "audit_logs")
public class AuditLogEntry {

    @Id
    private String id;

    private UUID actorUserId;

    private String action;

    private String targetType;

    private String targetId;

    private Map<String, String> metadata;

    private Instant occurredAt;

    protected AuditLogEntry() {
    }

    public AuditLogEntry(UUID actorUserId, String action, String targetType, String targetId,
            Map<String, String> metadata, Instant occurredAt) {
        this.actorUserId = actorUserId;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.metadata = metadata;
        this.occurredAt = occurredAt;
    }

    public String getId() {
        return id;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public String getAction() {
        return action;
    }

    public String getTargetType() {
        return targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
