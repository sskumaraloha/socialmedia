package com.socialmedia.message.event.outgoing;

import java.time.Instant;
import java.util.UUID;

public record MessageDeletedEvent(UUID messageId, UUID chatId, UUID deletedBy, Instant occurredAt) {
}
