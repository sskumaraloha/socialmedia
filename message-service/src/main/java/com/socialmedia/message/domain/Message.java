package com.socialmedia.message.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "messages")
public class Message {

    @Id
    private UUID id;

    @Indexed
    private UUID chatId;

    private UUID senderId;

    private MessageType type;

    /** Encrypted at rest by MessageEncryptionService before every save - never stored in plaintext. */
    private String content;

    private MediaMetadata media;

    private UUID replyToMessageId;

    private UUID forwardedFromMessageId;

    private List<Reaction> reactions = new ArrayList<>();

    private List<Receipt> receipts = new ArrayList<>();

    private boolean edited;

    private Instant editedAt;

    private boolean deletedForEveryone;

    private Instant deletedAt;

    private Set<UUID> deletedForUserIds = new HashSet<>();

    private Instant scheduledAt;

    private boolean sent;

    private Instant selfDestructAt;

    private boolean selfDestructed;

    @Indexed
    private Instant createdAt;

    private Instant updatedAt;

    protected Message() {
    }

    public Message(UUID chatId, UUID senderId, MessageType type, String content, MediaMetadata media,
            UUID replyToMessageId, UUID forwardedFromMessageId, Instant scheduledAt, Instant selfDestructAt) {
        this.id = UUID.randomUUID();
        this.chatId = chatId;
        this.senderId = senderId;
        this.type = type;
        this.content = content;
        this.media = media;
        this.replyToMessageId = replyToMessageId;
        this.forwardedFromMessageId = forwardedFromMessageId;
        this.scheduledAt = scheduledAt;
        this.sent = scheduledAt == null;
        this.selfDestructAt = selfDestructAt;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getChatId() {
        return chatId;
    }

    public UUID getSenderId() {
        return senderId;
    }

    public MessageType getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public MediaMetadata getMedia() {
        return media;
    }

    public UUID getReplyToMessageId() {
        return replyToMessageId;
    }

    public UUID getForwardedFromMessageId() {
        return forwardedFromMessageId;
    }

    public List<Reaction> getReactions() {
        return reactions;
    }

    public List<Receipt> getReceipts() {
        return receipts;
    }

    public boolean isEdited() {
        return edited;
    }

    public Instant getEditedAt() {
        return editedAt;
    }

    public boolean isDeletedForEveryone() {
        return deletedForEveryone;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public Set<UUID> getDeletedForUserIds() {
        return deletedForUserIds;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public boolean isSent() {
        return sent;
    }

    public Instant getSelfDestructAt() {
        return selfDestructAt;
    }

    public boolean isSelfDestructed() {
        return selfDestructed;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }

    public void edit(String newContent) {
        this.content = newContent;
        this.edited = true;
        this.editedAt = Instant.now();
        touch();
    }

    public void deleteForEveryone() {
        this.deletedForEveryone = true;
        this.deletedAt = Instant.now();
        this.content = null;
        this.media = null;
        touch();
    }

    public void deleteForUser(UUID userId) {
        this.deletedForUserIds.add(userId);
        touch();
    }

    public boolean isVisibleTo(UUID userId) {
        return !deletedForEveryone && !selfDestructed && !deletedForUserIds.contains(userId);
    }

    public void upsertReaction(UUID userId, String emoji) {
        reactions.removeIf(r -> r.getUserId().equals(userId));
        reactions.add(new Reaction(userId, emoji, Instant.now()));
        touch();
    }

    public void removeReaction(UUID userId) {
        reactions.removeIf(r -> r.getUserId().equals(userId));
        touch();
    }

    public Receipt receiptFor(UUID userId) {
        return receipts.stream()
                .filter(r -> r.getUserId().equals(userId))
                .findFirst()
                .orElseGet(() -> {
                    Receipt receipt = new Receipt(userId);
                    receipts.add(receipt);
                    return receipt;
                });
    }

    public void markSent() {
        this.sent = true;
        touch();
    }

    public void selfDestruct() {
        this.selfDestructed = true;
        this.content = null;
        this.media = null;
        touch();
    }

    public boolean isDueToPublish(Instant now) {
        return !sent && scheduledAt != null && !scheduledAt.isAfter(now);
    }

    public boolean isDueToSelfDestruct(Instant now) {
        return !selfDestructed && selfDestructAt != null && !selfDestructAt.isAfter(now);
    }
}
