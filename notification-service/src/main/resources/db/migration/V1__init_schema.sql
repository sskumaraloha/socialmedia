-- Baseline schema for notification-service. Hibernate runs with ddl-auto=validate, so
-- this migration (and only this migration) is the source of truth for the schema shape.

CREATE TABLE device_tokens (
    id          UUID PRIMARY KEY,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    user_id     UUID NOT NULL,
    token       VARCHAR(500) NOT NULL,
    platform    VARCHAR(20) NOT NULL,
    CONSTRAINT uq_device_tokens_user_token UNIQUE (user_id, token)
);
CREATE INDEX idx_device_tokens_user_id ON device_tokens (user_id);

CREATE TABLE notification_logs (
    id             UUID PRIMARY KEY,
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL,
    user_id        UUID NOT NULL,
    channel        VARCHAR(20) NOT NULL,
    template_key   VARCHAR(100) NOT NULL,
    status         VARCHAR(20) NOT NULL,
    error_message  VARCHAR(1000)
);
CREATE INDEX idx_notification_logs_user_id ON notification_logs (user_id);
