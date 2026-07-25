package com.socialmedia.chat.domain;

import com.socialmedia.common.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chats")
public class Chat extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatType type;

    @Column(length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(length = 1000)
    private String avatarUrl;

    @Column(nullable = false)
    private UUID createdBy;

    /** Only meaningful for CHANNEL: when true, only OWNER/ADMIN members may post messages. */
    @Column(nullable = false)
    private boolean broadcastOnly;

    private Instant lastMessageAt;

    @Column(length = 300)
    private String lastMessagePreview;

    protected Chat() {
    }

    public Chat(ChatType type, String name, String description, UUID createdBy, boolean broadcastOnly) {
        this.type = type;
        this.name = name;
        this.description = description;
        this.createdBy = createdBy;
        this.broadcastOnly = broadcastOnly;
    }

    public ChatType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public boolean isBroadcastOnly() {
        return broadcastOnly;
    }

    public Instant getLastMessageAt() {
        return lastMessageAt;
    }

    public String getLastMessagePreview() {
        return lastMessagePreview;
    }

    public void recordIncomingMessage(Instant sentAt, String preview) {
        this.lastMessageAt = sentAt;
        this.lastMessagePreview = preview;
    }
}
