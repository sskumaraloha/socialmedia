package com.socialmedia.auth.domain;

import com.socialmedia.common.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A single-use public prekey a device uploaded ahead of time, consumed exactly once when
 * another device fetches this device's key bundle to start a new session (see
 * DeviceKeyServiceImpl.getKeyBundle) - real X3DH's forward-secrecy guarantee for the very
 * first message in a session depends on each one-time prekey only ever being used once.
 */
@Entity
@Table(name = "one_time_pre_keys",
        indexes = @Index(name = "idx_one_time_pre_keys_device_id", columnList = "deviceId"),
        uniqueConstraints = @UniqueConstraint(name = "uq_one_time_pre_keys_device_key", columnNames = {"deviceId", "keyId"}))
public class OneTimePreKey extends BaseEntity {

    @Column(nullable = false)
    private String deviceId;

    @Column(name = "key_id", nullable = false)
    private int keyId;

    @Column(name = "public_key", nullable = false, columnDefinition = "BYTEA")
    private byte[] publicKey;

    @Column(nullable = false)
    private boolean consumed = false;

    protected OneTimePreKey() {
    }

    public OneTimePreKey(String deviceId, int keyId, byte[] publicKey) {
        this.deviceId = deviceId;
        this.keyId = keyId;
        this.publicKey = publicKey;
    }

    public void markConsumed() {
        this.consumed = true;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public int getKeyId() {
        return keyId;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public boolean isConsumed() {
        return consumed;
    }
}
