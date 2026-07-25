package com.socialmedia.chat.domain;

import com.socialmedia.common.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_members", uniqueConstraints = @UniqueConstraint(columnNames = {"chat_id", "user_id"}))
public class ChatMember extends BaseEntity {

    @Column(nullable = false)
    private UUID chatId;

    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatMemberRole role;

    @Column(nullable = false)
    private boolean muted;

    private Instant mutedUntil;

    @Column(nullable = false)
    private boolean archived;

    private Instant lastReadAt;

    @Column(nullable = false)
    private int unreadCount;

    protected ChatMember() {
    }

    public ChatMember(UUID chatId, UUID userId, ChatMemberRole role) {
        this.chatId = chatId;
        this.userId = userId;
        this.role = role;
        this.lastReadAt = Instant.now();
    }

    public UUID getChatId() {
        return chatId;
    }

    public UUID getUserId() {
        return userId;
    }

    public ChatMemberRole getRole() {
        return role;
    }

    public void setRole(ChatMemberRole role) {
        this.role = role;
    }

    public boolean isMuted() {
        return muted || (mutedUntil != null && mutedUntil.isAfter(Instant.now()));
    }

    public Instant getMutedUntil() {
        return mutedUntil;
    }

    public void mute(Instant until) {
        if (until == null) {
            this.muted = true;
            this.mutedUntil = null;
        } else {
            this.muted = false;
            this.mutedUntil = until;
        }
    }

    public void unmute() {
        this.muted = false;
        this.mutedUntil = null;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public Instant getLastReadAt() {
        return lastReadAt;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void markRead() {
        this.lastReadAt = Instant.now();
        this.unreadCount = 0;
    }

    public void incrementUnread() {
        this.unreadCount++;
    }
}
