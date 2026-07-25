package com.socialmedia.message.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ReceiptResponse(UUID userId, Instant deliveredAt, Instant readAt) {
}
