package com.socialmedia.message.domain;

import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A local, eventually-consistent mirror of chat-service's ChatMember rows, kept current by
 * consuming chat-service's Kafka events. Exists purely so message-service can fan out
 * typing-indicator / new-message WebSocket notifications without a synchronous call to
 * chat-service on every keystroke - NOT used for the send-authorization check (see
 * ChatMembershipClient), which needs the authoritative, immediately-consistent answer.
 */
@Document(collection = "chat_membership_mirror")
@CompoundIndex(name = "uq_chat_user", def = "{'chatId': 1, 'userId': 1}", unique = true)
public class ChatMembership {

    @Id
    private UUID id;

    @Indexed
    private UUID chatId;

    private UUID userId;

    protected ChatMembership() {
    }

    public ChatMembership(UUID chatId, UUID userId) {
        this.id = UUID.randomUUID();
        this.chatId = chatId;
        this.userId = userId;
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
}
