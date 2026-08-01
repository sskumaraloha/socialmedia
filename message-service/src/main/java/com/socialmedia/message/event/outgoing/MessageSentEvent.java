package com.socialmedia.message.event.outgoing;

import java.time.Instant;
import java.util.UUID;

/**
 * Must match the event.incoming.MessageSentEvent mirror in every consuming service
 * (chat-service, search-service, ai-service, analytics-service) field-for-field.
 *
 * <p>The {@code contentPreview} field this record used to carry is deliberately gone. It existed
 * only because the server held the message-encryption key and could produce a plaintext excerpt;
 * with real end-to-end encryption it cannot, so publishing it is not merely undesirable but
 * impossible. Every consumer that relied on it has been updated - see the README's
 * "End-to-end encryption" section for the full list of downstream consequences.
 */
public record MessageSentEvent(UUID messageId, UUID chatId, UUID senderId, Instant sentAt) {
}
