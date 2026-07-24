package com.socialmedia.auth.domain;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * QR login is inherently short-lived (~90s) and high-churn, so unlike the other domain
 * types this is a plain value object persisted in Redis (see QrLoginService), not a JPA
 * entity - there is no lasting business value in keeping it in Postgres once it expires.
 */
public class QrLoginSession implements Serializable {

    private String qrToken;
    private QrLoginStatus status;
    /** The unauthenticated (e.g. web) device that generated the QR code and will receive the session. */
    private String requestingDeviceId;
    private UUID userId;
    /** The already-authenticated (e.g. mobile) device that scanned and approved it - audit only. */
    private String approvedByDeviceId;
    private Instant createdAt;
    private Instant expiresAt;

    public QrLoginSession() {
    }

    public QrLoginSession(String qrToken, QrLoginStatus status, String requestingDeviceId, Instant createdAt,
            Instant expiresAt) {
        this.qrToken = qrToken;
        this.status = status;
        this.requestingDeviceId = requestingDeviceId;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public String getRequestingDeviceId() {
        return requestingDeviceId;
    }

    public void setRequestingDeviceId(String requestingDeviceId) {
        this.requestingDeviceId = requestingDeviceId;
    }

    public String getQrToken() {
        return qrToken;
    }

    public void setQrToken(String qrToken) {
        this.qrToken = qrToken;
    }

    public QrLoginStatus getStatus() {
        return status;
    }

    public void setStatus(QrLoginStatus status) {
        this.status = status;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getApprovedByDeviceId() {
        return approvedByDeviceId;
    }

    public void setApprovedByDeviceId(String approvedByDeviceId) {
        this.approvedByDeviceId = approvedByDeviceId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return expiresAt.isBefore(Instant.now());
    }
}
