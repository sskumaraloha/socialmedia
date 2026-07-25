package com.socialmedia.chat.event.outgoing;

import com.socialmedia.chat.domain.ChatType;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record ChatCreatedEvent(UUID chatId, ChatType type, String name, UUID createdBy, Set<UUID> memberIds, Instant occurredAt) {
}
