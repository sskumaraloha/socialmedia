package com.socialmedia.user.domain;

import com.socialmedia.common.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "muted_users", indexes = {
        @Index(name = "idx_muted_users_muter_id", columnList = "muterId"),
        @Index(name = "idx_muted_users_pair", columnList = "muterId,mutedId", unique = true)
})
public class MutedUser extends BaseEntity {

    @Column(nullable = false)
    private UUID muterId;

    @Column(nullable = false)
    private UUID mutedId;

    /** Null means muted indefinitely, until explicitly unmuted. */
    private Instant mutedUntil;

    protected MutedUser() {
    }

    public MutedUser(UUID muterId, UUID mutedId, Instant mutedUntil) {
        this.muterId = muterId;
        this.mutedId = mutedId;
        this.mutedUntil = mutedUntil;
    }

    public UUID getMuterId() {
        return muterId;
    }

    public UUID getMutedId() {
        return mutedId;
    }

    public Instant getMutedUntil() {
        return mutedUntil;
    }

    public boolean isActive() {
        return mutedUntil == null || mutedUntil.isAfter(Instant.now());
    }
}
