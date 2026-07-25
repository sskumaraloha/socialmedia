package com.socialmedia.ai.event.outgoing;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MessageModeratedEvent(UUID messageId, UUID chatId, UUID senderId, boolean flagged, boolean spam,
        List<String> categories, String reason, Instant occurredAt) {
}
