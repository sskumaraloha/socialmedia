package com.socialmedia.analytics.event.incoming;

import java.time.Instant;
import java.util.UUID;

/** Forward-declared: no payment-service exists in this platform yet. Shape is a reasonable guess
 * for whenever one is built, so the revenue dashboard has a real consumer waiting for it. */
public record PaymentCompletedEvent(UUID userId, UUID paymentId, long amountCents, String currency,
        Instant occurredAt) {
}
