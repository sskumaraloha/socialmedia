package com.socialmedia.search.event.incoming;

import java.time.Instant;
import java.util.UUID;

/** Local mirror of chat-service's outgoing event.ChatUpdatedEvent. */
public record ChatUpdatedEvent(UUID chatId, String name, String description, String avatarUrl, Instant occurredAt) {
}
