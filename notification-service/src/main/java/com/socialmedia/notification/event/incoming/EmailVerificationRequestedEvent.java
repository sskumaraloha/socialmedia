package com.socialmedia.notification.event.incoming;

import java.time.Instant;
import java.util.UUID;

/** Local mirror of auth-service's outgoing event.EmailVerificationRequestedEvent. */
public record EmailVerificationRequestedEvent(UUID userId, String email, String verificationToken, Instant occurredAt) {
}
