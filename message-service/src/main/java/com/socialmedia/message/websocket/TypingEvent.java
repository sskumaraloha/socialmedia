package com.socialmedia.message.websocket;

import java.time.Instant;
import java.util.UUID;

public record TypingEvent(UUID chatId, UUID userId, Instant occurredAt) {
}
