package com.socialmedia.user.domain;

import com.socialmedia.common.jpa.TimestampedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Keyed by the SAME UUID auth-service assigned that user (see UserRegisteredEventConsumer)
 * rather than a locally generated id - this is a service-local projection of an identity
 * that's authoritative in auth-service, not a new aggregate root.
 */
@Entity
@Table(name = "user_profiles", indexes = {
        @Index(name = "idx_user_profiles_username", columnList = "username", unique = true)
})
public class UserProfile extends TimestampedEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 32)
    private String username;

    @Column(length = 100)
    private String displayName;

    @Column(length = 500)
    private String bio;

    private String avatarUrl;

    @Column(nullable = false)
    private boolean verified = false;

    @Column(length = 100)
    private String customStatus;

    /** Best-effort cache updated by consuming presence-service's Kafka events; not the source of truth. */
    private Instant lastKnownOnlineAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Visibility onlineStatusVisibility = Visibility.EVERYONE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Visibility lastSeenVisibility = Visibility.EVERYONE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Visibility messageableBy = Visibility.EVERYONE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Visibility addableToGroupsBy = Visibility.EVERYONE;

    protected UserProfile() {
    }

    public UserProfile(UUID id, String username) {
        this.id = id;
        this.username = username;
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public String getCustomStatus() {
        return customStatus;
    }

    public void setCustomStatus(String customStatus) {
        this.customStatus = customStatus;
    }

    public Instant getLastKnownOnlineAt() {
        return lastKnownOnlineAt;
    }

    public void setLastKnownOnlineAt(Instant lastKnownOnlineAt) {
        this.lastKnownOnlineAt = lastKnownOnlineAt;
    }

    public Visibility getOnlineStatusVisibility() {
        return onlineStatusVisibility;
    }

    public void setOnlineStatusVisibility(Visibility onlineStatusVisibility) {
        this.onlineStatusVisibility = onlineStatusVisibility;
    }

    public Visibility getLastSeenVisibility() {
        return lastSeenVisibility;
    }

    public void setLastSeenVisibility(Visibility lastSeenVisibility) {
        this.lastSeenVisibility = lastSeenVisibility;
    }

    public Visibility getMessageableBy() {
        return messageableBy;
    }

    public void setMessageableBy(Visibility messageableBy) {
        this.messageableBy = messageableBy;
    }

    public Visibility getAddableToGroupsBy() {
        return addableToGroupsBy;
    }

    public void setAddableToGroupsBy(Visibility addableToGroupsBy) {
        this.addableToGroupsBy = addableToGroupsBy;
    }
}
