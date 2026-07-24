package com.socialmedia.auth.event;

import java.time.Instant;
import java.util.UUID;

public record PasswordResetRequestedEvent(UUID userId, String email, String resetToken, Instant occurredAt) {
}
