package com.socialmedia.message.dto.response;

import com.socialmedia.message.domain.MessageType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID chatId,
        UUID senderId,
        MessageType type,
        String content,
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
