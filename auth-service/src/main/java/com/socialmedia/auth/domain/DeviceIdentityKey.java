package com.socialmedia.auth.domain;

import com.socialmedia.common.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * One row per device, holding only PUBLIC key material for Signal-protocol PQXDH session
 * establishment: the identity public key, a signed prekey, and a Kyber (ML-KEM) prekey - both
 * prekeys signed by the identity key to prove provenance, and both rotated periodically. Never
 * holds a private key: those are generated and kept only on the client device, which is what
 * makes the resulting encryption end-to-end rather than server-mediated.
 *
 * <p>The Kyber prekey makes the key agreement post-quantum hybrid (X25519 + ML-KEM-1024) rather
 * than classical-only X3DH, matching Signal's own PQXDH upgrade. It is required, not optional:
 * libsignal cannot construct a PreKeyBundle without one.
 */
@Entity
@Table(name = "device_identity_keys", indexes = {
        @Index(name = "idx_device_identity_keys_device_id", columnList = "deviceId", unique = true)
})
public class DeviceIdentityKey extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String deviceId;

    /** libsignal's own per-device registration id, which a peer needs from the bundle. */
    @Column(name = "registration_id", nullable = false)
    private int registrationId;

    @Column(name = "identity_public_key", nullable = false, columnDefinition = "BYTEA")
    private byte[] identityPublicKey;

    @Column(name = "signed_pre_key_id", nullable = false)
    private int signedPreKeyId;

    @Column(name = "signed_pre_key_public", nullable = false, columnDefinition = "BYTEA")
    private byte[] signedPreKeyPublic;

    @Column(name = "signed_pre_key_signature", nullable = false, columnDefinition = "BYTEA")
    private byte[] signedPreKeySignature;

    @Column(name = "kyber_pre_key_id", nullable = false)
    private int kyberPreKeyId;

    @Column(name = "kyber_pre_key_public", nullable = false, columnDefinition = "BYTEA")
    private byte[] kyberPreKeyPublic;

    @Column(name = "kyber_pre_key_signature", nullable = false, columnDefinition = "BYTEA")
    private byte[] kyberPreKeySignature;

    @Column(name = "signed_pre_key_rotated_at", nullable = false)
    private Instant signedPreKeyRotatedAt;

    protected DeviceIdentityKey() {
    }

    public DeviceIdentityKey(String deviceId, int registrationId, byte[] identityPublicKey,
            int signedPreKeyId, byte[] signedPreKeyPublic, byte[] signedPreKeySignature,
            int kyberPreKeyId, byte[] kyberPreKeyPublic, byte[] kyberPreKeySignature) {
        this.deviceId = deviceId;
        this.registrationId = registrationId;
        this.identityPublicKey = identityPublicKey;
        this.signedPreKeyId = signedPreKeyId;
        this.signedPreKeyPublic = signedPreKeyPublic;
        this.signedPreKeySignature = signedPreKeySignature;
        this.kyberPreKeyId = kyberPreKeyId;
        this.kyberPreKeyPublic = kyberPreKeyPublic;
        this.kyberPreKeySignature = kyberPreKeySignature;
        this.signedPreKeyRotatedAt = Instant.now();
    }

    /** Prekeys rotate periodically (real clients do this every few days/weeks) - the identity
     * key itself does not change here; see DeviceKeyServiceImpl for why a changed identity key
     * is rejected rather than silently accepted. */
    public void rotatePreKeys(int signedPreKeyId, byte[] signedPreKeyPublic, byte[] signedPreKeySignature,
            int kyberPreKeyId, byte[] kyberPreKeyPublic, byte[] kyberPreKeySignature) {
        this.signedPreKeyId = signedPreKeyId;
        this.signedPreKeyPublic = signedPreKeyPublic;
        this.signedPreKeySignature = signedPreKeySignature;
        this.kyberPreKeyId = kyberPreKeyId;
        this.kyberPreKeyPublic = kyberPreKeyPublic;
        this.kyberPreKeySignature = kyberPreKeySignature;
        this.signedPreKeyRotatedAt = Instant.now();
    }

    public String getDeviceId() {
        return deviceId;
    }

    public int getRegistrationId() {
        return registrationId;
    }

    public byte[] getIdentityPublicKey() {
        return identityPublicKey;
    }

    public int getSignedPreKeyId() {
        return signedPreKeyId;
    }

    public byte[] getSignedPreKeyPublic() {
        return signedPreKeyPublic;
    }

    public byte[] getSignedPreKeySignature() {
        return signedPreKeySignature;
    }

    public int getKyberPreKeyId() {
        return kyberPreKeyId;
    }

    public byte[] getKyberPreKeyPublic() {
        return kyberPreKeyPublic;
    }

    public byte[] getKyberPreKeySignature() {
        return kyberPreKeySignature;
    }

    public Instant getSignedPreKeyRotatedAt() {
        return signedPreKeyRotatedAt;
    }
}
