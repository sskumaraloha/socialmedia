-- Baseline schema for media-service. Hibernate runs with ddl-auto=validate, so this
-- migration (and only this migration) is the source of truth for the schema shape.

CREATE TABLE media_assets (
    id                  UUID PRIMARY KEY,
    created_at          TIMESTAMPTZ NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL,
    owner_id            UUID NOT NULL,
    original_filename   VARCHAR(500) NOT NULL,
    mime_type           VARCHAR(200) NOT NULL,
    kind                VARCHAR(20) NOT NULL,
    size_bytes          BIGINT,
    storage_key         VARCHAR(1000) NOT NULL,
    thumbnail_key       VARCHAR(1000),
    status              VARCHAR(20) NOT NULL,
    virus_scan_status   VARCHAR(20) NOT NULL,
    upload_id           VARCHAR(500),
    transcoded          BOOLEAN NOT NULL DEFAULT FALSE,
    failure_reason      VARCHAR(500)
);
CREATE INDEX idx_media_assets_owner_id ON media_assets (owner_id);
