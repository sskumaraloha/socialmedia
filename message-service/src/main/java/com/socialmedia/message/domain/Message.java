package com.socialmedia.message.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
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

    /**
     * One client-encrypted ciphertext per recipient device (see EncryptedEnvelope). This service
     * holds no key for any of them - unlike the previous server-side-AES design, the plaintext is
     * genuinely unavailable here, which is what makes the platform end-to-end encrypted.
     */
    private List<EncryptedEnvelope> envelopes = new ArrayList<>();

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

    public Message(UUID chatId, UUID senderId, MessageType type, List<EncryptedEnvelope> envelopes, MediaMetadata media,
            UUID replyToMessageId, UUID forwardedFromMessageId, Instant scheduledAt, Instant selfDestructAt) {
        this.id = UUID.randomUUID();
        this.chatId = chatId;
        this.senderId = senderId;
        this.type = type;
        this.envelopes = new ArrayList<>(envelopes);
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

    public List<EncryptedEnvelope> getEnvelopes() {
        return envelopes;
    }

    /** The ciphertext addressed to one specific device, or empty if this message was never
     * encrypted for it (e.g. a device that joined the account after the message was sent -
     * genuinely unreadable by it, and the server cannot re-encrypt to fix that). */
    public Optional<EncryptedEnvelope> envelopeFor(String deviceId) {
        return envelopes.stream().filter(e -> e.getRecipientDeviceId().equals(deviceId)).findFirst();
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

    public void edit(List<EncryptedEnvelope> newEnvelopes) {
        this.envelopes = new ArrayList<>(newEnvelopes);
        this.edited = true;
        this.editedAt = Instant.now();
        touch();
    }

    public void deleteForEveryone() {
        this.deletedForEveryone = true;
        this.deletedAt = Instant.now();
        this.envelopes.clear();
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
        this.envelopes.clear();
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
