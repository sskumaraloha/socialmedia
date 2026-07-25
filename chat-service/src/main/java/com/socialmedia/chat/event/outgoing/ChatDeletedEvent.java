package com.socialmedia.chat.event.outgoing;

import java.time.Instant;
import java.util.UUID;

public record ChatDeletedEvent(UUID chatId, UUID deletedBy, Instant occurredAt) {
}
