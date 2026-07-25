package com.socialmedia.analytics.event.incoming;

import java.time.Instant;
import java.util.UUID;

/** Must match message-service's event.outgoing.MessageSentEvent field-for-field. */
public record MessageSentEvent(UUID messageId, UUID chatId, UUID senderId, String contentPreview, Instant sentAt) {
}
