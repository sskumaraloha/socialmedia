package com.socialmedia.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * One row per active login (one per device). The authoritative "is this session still
 * alive" check happens against Redis (see SessionCacheService) for low-latency access-token
 * validation; this table is the durable system of record used to list/revoke sessions
 * and survive a Redis flush.
 */
@Entity
@Table(name = "sessions", indexes = {
        @Index(name = "idx_sessions_user_id", columnList = "user_id"),
        @Index(name = "idx_sessions_jti", columnList = "jti", unique = true)
})
public class Session extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true)
    private String jti;

    @Column(nullable = false)
    private String deviceId;

    private String ipAddress;

    @Column(length = 512)
    private String userAgent;

    @Column(nullable = false)
    private Instant issuedAt;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Instant lastSeenAt;

    @Column(nullable = false)
    private boolean revoked = false;

    protected Session() {
    }

    public Session(User user, String jti, String deviceId, String ipAddress, String userAgent,
            Instant issuedAt, Instant expiresAt) {
        this.user = user;
        this.jti = jti;
        this.deviceId = deviceId;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.lastSeenAt = issuedAt;
    }

    public User getUser() {
        return user;
    }

    public String getJti() {
        return jti;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void touch() {
        this.lastSeenAt = Instant.now();
    }

    /** Called on refresh-token rotation - the session stays the same "login", just with a new access token. */
    public void rotate(String newJti, Instant issuedAt, Instant expiresAt) {
        this.jti = newJti;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.lastSeenAt = issuedAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }
}
