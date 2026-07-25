package com.socialmedia.ai.event.incoming;

import java.time.Instant;
import java.util.UUID;

/** Local mirror of message-service's outgoing event.MessageSentEvent. */
public record MessageSentEvent(UUID messageId, UUID chatId, UUID senderId, String contentPreview, Instant sentAt) {
}
