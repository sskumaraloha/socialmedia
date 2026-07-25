package com.socialmedia.message.domain;

import java.time.Instant;
import java.util.UUID;

/** Embedded in Message - per-recipient delivery/read timestamps. */
public class Receipt {

    private UUID userId;
    private Instant deliveredAt;
    private Instant readAt;

    protected Receipt() {
    }

    public Receipt(UUID userId) {
        this.userId = userId;
    }

    public UUID getUserId() {
        return userId;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public void markDelivered(Instant at) {
        if (this.deliveredAt == null) {
            this.deliveredAt = at;
        }
    }

    public void markRead(Instant at) {
        markDelivered(at);
        this.readAt = at;
    }
}
