package com.socialmedia.search.event.incoming;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Local mirror of chat-service's outgoing event.ChatCreatedEvent. */
public record ChatCreatedEvent(UUID chatId, String type, String name, UUID createdBy, Set<UUID> memberIds, Instant occurredAt) {
}
