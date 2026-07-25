package com.socialmedia.message.domain;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "starred_messages")
@CompoundIndex(name = "uq_user_message", def = "{'userId': 1, 'messageId': 1}", unique = true)
public class StarredMessage {

    @Id
    private UUID id;

    private UUID userId;

    private UUID messageId;

    private Instant starredAt;

    protected StarredMessage() {
    }

    public StarredMessage(UUID userId, UUID messageId) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.messageId = messageId;
        this.starredAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public Instant getStarredAt() {
        return starredAt;
    }
}
