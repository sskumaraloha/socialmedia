package com.socialmedia.message.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ReactionResponse(UUID userId, String emoji, Instant reactedAt) {
}
