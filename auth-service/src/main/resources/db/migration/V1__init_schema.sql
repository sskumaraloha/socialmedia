-- Baseline schema for auth-service. Hibernate runs with ddl-auto=validate, so this
-- migration (and only this migration) is the source of truth for the schema shape.

CREATE TABLE users (
    id                     UUID PRIMARY KEY,
    created_at             TIMESTAMPTZ NOT NULL,
    updated_at             TIMESTAMPTZ NOT NULL,
    email                  VARCHAR(255) NOT NULL,
    password_hash          VARCHAR(255),
    email_verified         BOOLEAN NOT NULL DEFAULT FALSE,
    enabled                BOOLEAN NOT NULL DEFAULT TRUE,
    account_locked         BOOLEAN NOT NULL DEFAULT FALSE,
    locked_until           TIMESTAMPTZ,
    failed_login_attempts  INT NOT NULL DEFAULT 0,
    provider               VARCHAR(20) NOT NULL,
    provider_id            VARCHAR(255),
    two_factor_enabled     BOOLEAN NOT NULL DEFAULT FALSE,
    two_factor_secret      VARCHAR(255),
    CONSTRAINT uq_users_email UNIQUE (email)
);
CREATE INDEX idx_users_provider_provider_id ON users (provider, provider_id);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role    VARCHAR(20) NOT NULL,
    PRIMARY KEY (user_id, role)
);

CREATE TABLE devices (
    id              UUID PRIMARY KEY,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    user_id         UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    device_id       VARCHAR(255) NOT NULL,
    device_name     VARCHAR(255),
    device_type     VARCHAR(20) NOT NULL,
    push_token      VARCHAR(500),
    last_active_at  TIMESTAMPTZ NOT NULL,
    trusted         BOOLEAN NOT NULL DEFAULT FALSE,
    revoked         BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_devices_device_id UNIQUE (device_id)
);
CREATE INDEX idx_devices_user_id ON devices (user_id);

CREATE TABLE refresh_tokens (
    id                       UUID PRIMARY KEY,
    created_at               TIMESTAMPTZ NOT NULL,
    updated_at               TIMESTAMPTZ NOT NULL,
    user_id                  UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash               VARCHAR(255) NOT NULL,
    device_id                VARCHAR(255) NOT NULL,
    expires_at               TIMESTAMPTZ NOT NULL,
    revoked                  BOOLEAN NOT NULL DEFAULT FALSE,
    replaced_by_token_hash   VARCHAR(255),
    CONSTRAINT uq_refresh_tokens_token_hash UNIQUE (token_hash)
);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);

CREATE TABLE sessions (
    id             UUID PRIMARY KEY,
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL,
    user_id        UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    jti            VARCHAR(255) NOT NULL,
    device_id      VARCHAR(255) NOT NULL,
    ip_address     VARCHAR(64),
    user_agent     VARCHAR(512),
    issued_at      TIMESTAMPTZ NOT NULL,
    expires_at     TIMESTAMPTZ NOT NULL,
    last_seen_at   TIMESTAMPTZ NOT NULL,
    revoked        BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_sessions_jti UNIQUE (jti)
);
CREATE INDEX idx_sessions_user_id ON sessions (user_id);
CREATE INDEX idx_sessions_user_device ON sessions (user_id, device_id);

CREATE TABLE login_history (
    id              UUID PRIMARY KEY,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    user_id         UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    ip_address      VARCHAR(64),
    user_agent      VARCHAR(512),
    success         BOOLEAN NOT NULL,
    failure_reason  VARCHAR(255)
);
CREATE INDEX idx_login_history_user_id ON login_history (user_id);

CREATE TABLE audit_logs (
    id              UUID PRIMARY KEY,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    actor_user_id   UUID NOT NULL,
    action          VARCHAR(100) NOT NULL,
    target_type     VARCHAR(100),
    target_id       VARCHAR(255),
    metadata        VARCHAR(2000),
    ip_address      VARCHAR(64)
);
CREATE INDEX idx_audit_logs_actor_user_id ON audit_logs (actor_user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs (action);

CREATE TABLE email_verification_tokens (
    id          UUID PRIMARY KEY,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    user_id     UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    used        BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_evt_token_hash UNIQUE (token_hash)
);

CREATE TABLE password_reset_tokens (
    id          UUID PRIMARY KEY,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    user_id     UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    used        BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_prt_token_hash UNIQUE (token_hash)
);
