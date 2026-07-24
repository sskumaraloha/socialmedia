package com.socialmedia.auth.event;

import java.time.Instant;
import java.util.UUID;

/** Consumed by notification-service, which owns actually sending the email (see docs/ARCHITECTURE.md). */
public record EmailVerificationRequestedEvent(UUID userId, String email, String verificationToken, Instant occurredAt) {
}
