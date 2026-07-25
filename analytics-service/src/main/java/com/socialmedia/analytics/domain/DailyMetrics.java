package com.socialmedia.analytics.domain;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * One document per calendar day (UTC), incrementally aggregated as events arrive - this
 * is the platform's "Kafka stream processing" for analytics: a simplified, hand-rolled
 * continuous aggregation via plain @KafkaListener consumers making atomic MongoDB
 * increments, rather than a full Kafka Streams topology with state stores - a deliberate
 * scope choice, not an oversight (see the README).
 */
@Document(collection = "daily_metrics")
public class DailyMetrics {

    @Id
    private LocalDate date;

    private long newSignups;

    private Set<UUID> activeUserIds = new HashSet<>();

    private long messagesSent;

    private long storageBytesUploaded;

    private long revenueCents;

    protected DailyMetrics() {
    }

    public DailyMetrics(LocalDate date) {
        this.date = date;
    }

    public LocalDate getDate() {
        return date;
    }

    public long getNewSignups() {
        return newSignups;
    }

    public Set<UUID> getActiveUserIds() {
        return activeUserIds;
    }

    public long getMessagesSent() {
        return messagesSent;
    }

    public long getStorageBytesUploaded() {
        return storageBytesUploaded;
    }

    public long getRevenueCents() {
        return revenueCents;
    }
}
