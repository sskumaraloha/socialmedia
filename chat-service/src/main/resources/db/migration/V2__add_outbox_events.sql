-- Transactional outbox: every domain change that must publish a Kafka event stages it
-- here, in the SAME transaction as the domain write, instead of calling Kafka directly -
-- see OutboxEventPublisherScheduler for the (separate, at-least-once) poller that actually
-- delivers these to Kafka.

CREATE TABLE outbox_events (
    id             UUID PRIMARY KEY,
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL,
    topic          VARCHAR(100) NOT NULL,
    aggregate_key  VARCHAR(100) NOT NULL,
    event_type     VARCHAR(100) NOT NULL,
    payload        TEXT NOT NULL,
    published      BOOLEAN NOT NULL DEFAULT FALSE,
    published_at   TIMESTAMPTZ
);
CREATE INDEX idx_outbox_events_unpublished ON outbox_events (created_at) WHERE NOT published;
