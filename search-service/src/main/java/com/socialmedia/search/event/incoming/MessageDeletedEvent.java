package com.socialmedia.search.event.incoming;

import java.time.Instant;
import java.util.UUID;

/** Local mirror of message-service's outgoing event.MessageDeletedEvent. */
public record MessageDeletedEvent(UUID messageId, UUID chatId, UUID deletedBy, Instant occurredAt) {
}
