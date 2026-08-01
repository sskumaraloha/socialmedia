-- Public-key infrastructure for end-to-end encryption. This is deliberately a dumb public-bundle
-- directory, same as Signal's own server: only public key bytes are ever stored here, never a
-- private key or session/ratchet state - those live only on the client device, which is what
-- makes the encryption actually end-to-end.
--
-- The kyber_* columns are not optional extras: the current Signal protocol is PQXDH, a
-- post-quantum HYBRID key agreement (X25519 + ML-KEM/Kyber-1024), and libsignal requires a
-- Kyber prekey to build a PreKeyBundle at all. Carrying them here is what makes this directory
-- usable by a real client, and it means the platform is post-quantum-hybrid from day one rather
-- than needing a later migration.

CREATE TABLE device_identity_keys (
    id                          UUID PRIMARY KEY,
    created_at                  TIMESTAMPTZ NOT NULL,
    updated_at                  TIMESTAMPTZ NOT NULL,
    device_id                   VARCHAR(255) NOT NULL,
    registration_id             INTEGER NOT NULL,
    identity_public_key         BYTEA NOT NULL,
    signed_pre_key_id           INTEGER NOT NULL,
    signed_pre_key_public       BYTEA NOT NULL,
    signed_pre_key_signature    BYTEA NOT NULL,
    kyber_pre_key_id            INTEGER NOT NULL,
    kyber_pre_key_public        BYTEA NOT NULL,
    kyber_pre_key_signature     BYTEA NOT NULL,
    signed_pre_key_rotated_at   TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_device_identity_keys_device_id UNIQUE (device_id)
);

CREATE TABLE one_time_pre_keys (
    id           UUID PRIMARY KEY,
    created_at   TIMESTAMPTZ NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL,
    device_id    VARCHAR(255) NOT NULL,
    key_id       INTEGER NOT NULL,
    public_key   BYTEA NOT NULL,
    consumed     BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_one_time_pre_keys_device_key UNIQUE (device_id, key_id)
);
CREATE INDEX idx_one_time_pre_keys_device_id_unconsumed ON one_time_pre_keys (device_id) WHERE NOT consumed;
