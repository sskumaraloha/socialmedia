package com.socialmedia.search.domain;

import java.time.Instant;
import java.util.UUID;

/** Indexes the same truncated content preview message-service already publishes for
 * chat-service's benefit (message.sent.v1) - never the full decrypted content, and never
 * a separate read from message-service's own encrypted store. */
public record MessageDocument(UUID messageId, UUID chatId, UUID senderId, String contentPreview, Instant sentAt) {
}
