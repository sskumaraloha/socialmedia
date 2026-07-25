package com.socialmedia.chat.event.outgoing;

import java.time.Instant;
import java.util.UUID;

public record ChatUpdatedEvent(UUID chatId, String name, String description, String avatarUrl, Instant occurredAt) {
}
