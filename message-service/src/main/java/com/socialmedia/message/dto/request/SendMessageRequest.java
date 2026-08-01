package com.socialmedia.message.dto.request;

import com.socialmedia.message.domain.MessageType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * There is deliberately no plaintext {@code content} field any more: the client encrypts before
 * it ever calls this API, once per recipient device (including its own other devices, so the
 * sender's own history stays readable across their devices). The server never receives, and
 * therefore cannot log, index, or leak, the message text.
 */
public record SendMessageRequest(
        @NotNull MessageType type,
        @NotEmpty List<@Valid EncryptedEnvelopeRequest> envelopes,
        MediaRequest media,
        UUID replyToMessageId,
        Instant scheduledAt,
        @Positive Long selfDestructSeconds
) {
}
