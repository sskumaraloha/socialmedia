package com.socialmedia.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Immutable record of a security-relevant action, independent of LoginHistory (which is
 * login-attempt specific). Written for account changes: password reset, 2FA toggled,
 * device revoked, role changed, etc.
 */
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_logs_actor_user_id", columnList = "actorUserId"),
        @Index(name = "idx_audit_logs_action", columnList = "action")
})
public class AuditLog extends BaseEntity {

    @Column(nullable = false)
    private UUID actorUserId;

    @Column(nullable = false)
    private String action;

    private String targetType;

    private String targetId;

    @Column(length = 2000)
    private String metadata;

    private String ipAddress;

    protected AuditLog() {
    }

    public AuditLog(UUID actorUserId, String action, String targetType, String targetId, String metadata,
            String ipAddress) {
        this.actorUserId = actorUserId;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.metadata = metadata;
        this.ipAddress = ipAddress;
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

    public String getMetadata() {
        return metadata;
    }

    public String getIpAddress() {
        return ipAddress;
    }
}
