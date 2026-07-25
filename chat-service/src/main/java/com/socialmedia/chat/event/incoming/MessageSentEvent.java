package com.socialmedia.chat.event.incoming;

import java.time.Instant;
import java.util.UUID;

/**
 * Local mirror of message-service's outgoing event (topic message.sent.v1). message-service
 * doesn't exist yet, so this is the agreed wire contract chat-service reacts to in order to
 * bump unread counts and keep the chat list's last-message preview current.
 */
public record MessageSentEvent(UUID messageId, UUID chatId, UUID senderId, String contentPreview, Instant sentAt) {
}
