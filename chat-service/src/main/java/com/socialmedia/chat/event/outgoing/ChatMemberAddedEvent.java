package com.socialmedia.chat.event.outgoing;

import java.time.Instant;
import java.util.UUID;

public record ChatMemberAddedEvent(UUID chatId, UUID userId, UUID addedBy, Instant occurredAt) {
}
