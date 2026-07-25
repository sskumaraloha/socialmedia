-- Baseline schema for chat-service. Hibernate runs with ddl-auto=validate, so this
-- migration (and only this migration) is the source of truth for the schema shape.

CREATE TABLE chats (
    id                     UUID PRIMARY KEY,
    created_at             TIMESTAMPTZ NOT NULL,
    updated_at             TIMESTAMPTZ NOT NULL,
    type                   VARCHAR(20) NOT NULL,
    name                   VARCHAR(100),
    description            VARCHAR(500),
    avatar_url             VARCHAR(1000),
    created_by             UUID NOT NULL,
    broadcast_only         BOOLEAN NOT NULL DEFAULT FALSE,
    last_message_at        TIMESTAMPTZ,
    last_message_preview   VARCHAR(300)
);

CREATE TABLE chat_members (
    id             UUID PRIMARY KEY,
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL,
    chat_id        UUID NOT NULL,
    user_id        UUID NOT NULL,
    role           VARCHAR(20) NOT NULL,
    muted          BOOLEAN NOT NULL DEFAULT FALSE,
    muted_until    TIMESTAMPTZ,
    archived       BOOLEAN NOT NULL DEFAULT FALSE,
    last_read_at   TIMESTAMPTZ,
    unread_count   INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uq_chat_members_chat_user UNIQUE (chat_id, user_id)
);
CREATE INDEX idx_chat_members_chat_id ON chat_members (chat_id);
CREATE INDEX idx_chat_members_user_id ON chat_members (user_id);

CREATE TABLE pinned_messages (
    id           UUID PRIMARY KEY,
    created_at   TIMESTAMPTZ NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL,
    chat_id      UUID NOT NULL,
    message_id   UUID NOT NULL,
    pinned_by    UUID NOT NULL,
    CONSTRAINT uq_pinned_messages_chat_message UNIQUE (chat_id, message_id)
);
CREATE INDEX idx_pinned_messages_chat_id ON pinned_messages (chat_id);
