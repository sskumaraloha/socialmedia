package com.socialmedia.message.event.incoming;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Local mirror of chat-service's outgoing ChatCreatedEvent (topic chat.created.v1). */
public record ChatCreatedEvent(UUID chatId, Set<UUID> memberIds, Instant occurredAt) {
}
