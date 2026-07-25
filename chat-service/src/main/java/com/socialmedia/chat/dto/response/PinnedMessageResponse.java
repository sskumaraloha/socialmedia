package com.socialmedia.chat.dto.response;

import java.time.Instant;
import java.util.UUID;

public record PinnedMessageResponse(UUID messageId, UUID pinnedBy, Instant pinnedAt) {
}
