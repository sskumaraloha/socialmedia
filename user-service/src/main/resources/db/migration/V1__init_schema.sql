-- Baseline schema for user-service. Hibernate runs with ddl-auto=validate, so this
-- migration (and only this migration) is the source of truth for the schema shape.

CREATE TABLE user_profiles (
    id                          UUID PRIMARY KEY,
    created_at                  TIMESTAMPTZ NOT NULL,
    updated_at                  TIMESTAMPTZ NOT NULL,
    username                    VARCHAR(32) NOT NULL,
    display_name                VARCHAR(100),
    bio                         VARCHAR(500),
    avatar_url                  VARCHAR(1000),
    verified                    BOOLEAN NOT NULL DEFAULT FALSE,
    custom_status               VARCHAR(100),
    last_known_online_at        TIMESTAMPTZ,
    online_status_visibility    VARCHAR(20) NOT NULL DEFAULT 'EVERYONE',
    last_seen_visibility        VARCHAR(20) NOT NULL DEFAULT 'EVERYONE',
    messageable_by              VARCHAR(20) NOT NULL DEFAULT 'EVERYONE',
    addable_to_groups_by        VARCHAR(20) NOT NULL DEFAULT 'EVERYONE',
    CONSTRAINT uq_user_profiles_username UNIQUE (username)
);

CREATE TABLE follows (
    id            UUID PRIMARY KEY,
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL,
    follower_id   UUID NOT NULL,
    following_id  UUID NOT NULL,
    CONSTRAINT uq_follows_pair UNIQUE (follower_id, following_id)
);
CREATE INDEX idx_follows_follower_id ON follows (follower_id);
CREATE INDEX idx_follows_following_id ON follows (following_id);

CREATE TABLE contacts (
    id               UUID PRIMARY KEY,
    created_at       TIMESTAMPTZ NOT NULL,
    updated_at       TIMESTAMPTZ NOT NULL,
    owner_id         UUID NOT NULL,
    contact_user_id  UUID NOT NULL,
    nickname         VARCHAR(100),
    CONSTRAINT uq_contacts_pair UNIQUE (owner_id, contact_user_id)
);
CREATE INDEX idx_contacts_owner_id ON contacts (owner_id);

CREATE TABLE blocked_users (
    id          UUID PRIMARY KEY,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    blocker_id  UUID NOT NULL,
    blocked_id  UUID NOT NULL,
    CONSTRAINT uq_blocked_users_pair UNIQUE (blocker_id, blocked_id)
);
CREATE INDEX idx_blocked_users_blocker_id ON blocked_users (blocker_id);

CREATE TABLE muted_users (
    id           UUID PRIMARY KEY,
    created_at   TIMESTAMPTZ NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL,
    muter_id     UUID NOT NULL,
    muted_id     UUID NOT NULL,
    muted_until  TIMESTAMPTZ,
    CONSTRAINT uq_muted_users_pair UNIQUE (muter_id, muted_id)
);
CREATE INDEX idx_muted_users_muter_id ON muted_users (muter_id);
