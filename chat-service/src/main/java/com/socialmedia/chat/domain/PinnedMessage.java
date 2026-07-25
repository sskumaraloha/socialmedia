package com.socialmedia.chat.domain;

import com.socialmedia.common.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

@Entity
@Table(name = "pinned_messages", uniqueConstraints = @UniqueConstraint(columnNames = {"chat_id", "message_id"}))
public class PinnedMessage extends BaseEntity {

    @Column(nullable = false)
    private UUID chatId;

    /** Opaque reference to a message owned by message-service - chat-service never reads its content. */
    @Column(nullable = false)
    private UUID messageId;

    @Column(nullable = false)
    private UUID pinnedBy;

    protected PinnedMessage() {
    }

    public PinnedMessage(UUID chatId, UUID messageId, UUID pinnedBy) {
        this.chatId = chatId;
        this.messageId = messageId;
        this.pinnedBy = pinnedBy;
    }

    public UUID getChatId() {
        return chatId;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public UUID getPinnedBy() {
        return pinnedBy;
    }
}
