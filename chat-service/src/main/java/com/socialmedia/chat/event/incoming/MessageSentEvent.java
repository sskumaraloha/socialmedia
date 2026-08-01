package com.socialmedia.chat.event.incoming;

import java.time.Instant;
import java.util.UUID;

/**
 * Local mirror of message-service's outgoing event (topic message.sent.v1), consumed to bump
 * unread counts and last-activity timestamps.
 *
 * <p>The {@code contentPreview} this mirror used to carry is gone: messages are now end-to-end
 * encrypted, so message-service genuinely cannot produce a plaintext excerpt. The chat list's
 * "last message" preview is therefore a client-side concern now - each client already holds the
 * decrypted message and can render its own preview, which is exactly how WhatsApp and Signal do
 * it.
 */
public record MessageSentEvent(UUID messageId, UUID chatId, UUID senderId, Instant sentAt) {
}
