package com.socialmedia.message.dto.request;

import com.socialmedia.message.domain.MessageType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.UUID;

public record SendMessageRequest(
        @NotNull MessageType type,
        String content,
        MediaRequest media,
        UUID replyToMessageId,
        Instant scheduledAt,
        @Positive Long selfDestructSeconds
) {
}
