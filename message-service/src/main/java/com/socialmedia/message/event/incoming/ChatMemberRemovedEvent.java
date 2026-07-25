package com.socialmedia.message.event.incoming;

import java.time.Instant;
import java.util.UUID;

/** Local mirror of chat-service's outgoing ChatMemberRemovedEvent (topic chat.member.removed.v1). */
public record ChatMemberRemovedEvent(UUID chatId, UUID userId, Instant occurredAt) {
}
