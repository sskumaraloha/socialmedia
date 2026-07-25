package com.socialmedia.message.dto.response;

import java.time.Instant;
import java.util.UUID;

public record DraftResponse(UUID chatId, String content, Instant updatedAt) {
}
