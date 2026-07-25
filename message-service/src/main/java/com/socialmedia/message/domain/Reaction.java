package com.socialmedia.message.domain;

import java.time.Instant;
import java.util.UUID;

/** Embedded in Message - one active emoji per user, natural fit for a document rather than a join table. */
public class Reaction {

    private UUID userId;
    private String emoji;
    private Instant reactedAt;

    protected Reaction() {
    }

    public Reaction(UUID userId, String emoji, Instant reactedAt) {
        this.userId = userId;
        this.emoji = emoji;
        this.reactedAt = reactedAt;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getEmoji() {
        return emoji;
    }

    public Instant getReactedAt() {
        return reactedAt;
    }
}
