package com.socialmedia.chat.outbox;

import com.socialmedia.common.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String topic;

    @Column(name = "aggregate_key", nullable = false, length = 100)
    private String aggregateKey;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private boolean published;

    @Column(name = "published_at")
    private Instant publishedAt;

    protected OutboxEvent() {
    }

    public OutboxEvent(String topic, String aggregateKey, String eventType, String payload) {
        this.topic = topic;
        this.aggregateKey = aggregateKey;
        this.eventType = eventType;
        this.payload = payload;
        this.published = false;
    }

    public void markPublished() {
        this.published = true;
        this.publishedAt = Instant.now();
    }

    public String getTopic() {
        return topic;
    }

    public String getAggregateKey() {
        return aggregateKey;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public boolean isPublished() {
        return published;
    }
}
