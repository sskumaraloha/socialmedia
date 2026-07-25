package com.socialmedia.message.domain;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "drafts")
@CompoundIndex(name = "uq_chat_user", def = "{'chatId': 1, 'userId': 1}", unique = true)
public class Draft {

    @Id
    private UUID id;

    private UUID chatId;

    private UUID userId;

    private String content;

    private Instant updatedAt;

    protected Draft() {
    }

    public Draft(UUID chatId, UUID userId, String content) {
        this.id = UUID.randomUUID();
        this.chatId = chatId;
        this.userId = userId;
        this.content = content;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getChatId() {
        return chatId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getContent() {
        return content;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setContent(String content) {
        this.content = content;
        this.updatedAt = Instant.now();
    }
}
