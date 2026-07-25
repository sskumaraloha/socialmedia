package com.socialmedia.user.domain;

import com.socialmedia.common.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "follows", indexes = {
        @Index(name = "idx_follows_follower_id", columnList = "followerId"),
        @Index(name = "idx_follows_following_id", columnList = "followingId"),
        @Index(name = "idx_follows_pair", columnList = "followerId,followingId", unique = true)
})
public class Follow extends BaseEntity {

    @Column(nullable = false)
    private UUID followerId;

    @Column(nullable = false)
    private UUID followingId;

    protected Follow() {
    }

    public Follow(UUID followerId, UUID followingId) {
        this.followerId = followerId;
        this.followingId = followingId;
    }

    public UUID getFollowerId() {
        return followerId;
    }

    public UUID getFollowingId() {
        return followingId;
    }
}
