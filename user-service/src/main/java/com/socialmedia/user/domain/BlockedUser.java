package com.socialmedia.user.domain;

import com.socialmedia.common.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "blocked_users", indexes = {
        @Index(name = "idx_blocked_users_blocker_id", columnList = "blockerId"),
        @Index(name = "idx_blocked_users_pair", columnList = "blockerId,blockedId", unique = true)
})
public class BlockedUser extends BaseEntity {

    @Column(nullable = false)
    private UUID blockerId;

    @Column(nullable = false)
    private UUID blockedId;

    protected BlockedUser() {
    }

    public BlockedUser(UUID blockerId, UUID blockedId) {
        this.blockerId = blockerId;
        this.blockedId = blockedId;
    }

    public UUID getBlockerId() {
        return blockerId;
    }

    public UUID getBlockedId() {
        return blockedId;
    }
}
