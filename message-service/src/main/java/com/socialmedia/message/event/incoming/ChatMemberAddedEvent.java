package com.socialmedia.message.event.incoming;

import java.time.Instant;
import java.util.UUID;

/** Local mirror of chat-service's outgoing ChatMemberAddedEvent (topic chat.member.added.v1). */
public record ChatMemberAddedEvent(UUID chatId, UUID userId, Instant occurredAt) {
}
