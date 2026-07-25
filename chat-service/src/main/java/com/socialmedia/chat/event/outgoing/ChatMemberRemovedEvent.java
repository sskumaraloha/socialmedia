package com.socialmedia.chat.event.outgoing;

import java.time.Instant;
import java.util.UUID;

public record ChatMemberRemovedEvent(UUID chatId, UUID userId, UUID removedBy, Instant occurredAt) {
}
