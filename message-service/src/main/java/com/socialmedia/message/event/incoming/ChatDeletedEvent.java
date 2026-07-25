package com.socialmedia.message.event.incoming;

import java.time.Instant;
import java.util.UUID;

/** Local mirror of chat-service's outgoing ChatDeletedEvent (topic chat.deleted.v1). */
public record ChatDeletedEvent(UUID chatId, Instant occurredAt) {
}
