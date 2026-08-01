package com.socialmedia.message.dto.response;

import com.socialmedia.message.domain.MessageType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * {@code ciphertext}/{@code cipherType} carry only the envelope addressed to the requesting
 * device - the caller decrypts locally with its own Double Ratchet session state. Both are null
 * when this message holds no envelope for that device (it was deleted, self-destructed, or the
 * device simply wasn't a recipient), which the client should render as an unavailable message
 * rather than an error.
 */
public record MessageResponse(
        UUID id,
        UUID chatId,
        UUID senderId,
        MessageType type,
        Integer cipherType,
        String ciphertext,
        MediaResponse media,
        UUID replyToMessageId,
        UUID forwardedFromMessageId,
        List<ReactionResponse> reactions,
        List<ReceiptResponse> receipts,
        boolean edited,
        Instant editedAt,
        boolean deletedForEveryone,
        Instant scheduledAt,
        boolean sent,
        Instant selfDestructAt,
        Instant createdAt,
        Instant updatedAt
) {
}
