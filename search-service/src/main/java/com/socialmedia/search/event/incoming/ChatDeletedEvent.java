package com.socialmedia.search.event.incoming;

import java.time.Instant;
import java.util.UUID;

/** Local mirror of chat-service's outgoing event.ChatDeletedEvent. */
public record ChatDeletedEvent(UUID chatId, UUID deletedBy, Instant occurredAt) {
}
