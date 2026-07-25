package com.socialmedia.message.event.outgoing;

import java.time.Instant;
import java.util.UUID;

/** Must match chat-service's event.incoming.MessageSentEvent field-for-field. */
public record MessageSentEvent(UUID messageId, UUID chatId, UUID senderId, String contentPreview, Instant sentAt) {
}
