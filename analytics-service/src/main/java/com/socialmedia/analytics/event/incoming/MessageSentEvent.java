package com.socialmedia.analytics.event.incoming;

import java.time.Instant;
import java.util.UUID;

/** Must match message-service's event.outgoing.MessageSentEvent field-for-field. The
 * contentPreview field is gone platform-wide now that messages are end-to-end encrypted;
 * analytics only ever counted messages by timestamp, so nothing here actually loses capability. */
public record MessageSentEvent(UUID messageId, UUID chatId, UUID senderId, Instant sentAt) {
}
