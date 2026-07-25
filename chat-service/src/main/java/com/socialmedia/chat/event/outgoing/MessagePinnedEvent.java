package com.socialmedia.chat.event.outgoing;

import java.time.Instant;
import java.util.UUID;

public record MessagePinnedEvent(UUID chatId, UUID messageId, UUID pinnedBy, Instant occurredAt) {
}
