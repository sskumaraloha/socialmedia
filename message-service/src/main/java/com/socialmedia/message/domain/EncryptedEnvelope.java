package com.socialmedia.message.domain;

/**
 * One recipient device's copy of a message's ciphertext. Real Signal-protocol messaging is
 * per-device, not per-user: a sender runs a separate Double Ratchet session with every device
 * the conversation involves (including their own other devices, so their history syncs), so a
 * single logical message arrives at the server as N independently-encrypted envelopes. The
 * server stores all of them side by side and hands each device only its own - it cannot read
 * any of them.
 *
 * <p>{@code cipherType} is libsignal's own message-type discriminator (3 = PreKeySignalMessage,
 * the first message that establishes a session; 2 = SignalMessage, every message after that).
 * The server treats it as an opaque integer it must round-trip faithfully so the recipient's
 * client knows which decrypt path to take - it never interprets it.
 */
public class EncryptedEnvelope {

    private String recipientDeviceId;

    private int cipherType;

    /** Base64 of the client's serialized libsignal ciphertext. Opaque to this service. */
    private String ciphertext;

    protected EncryptedEnvelope() {
    }

    public EncryptedEnvelope(String recipientDeviceId, int cipherType, String ciphertext) {
        this.recipientDeviceId = recipientDeviceId;
        this.cipherType = cipherType;
        this.ciphertext = ciphertext;
    }

    public String getRecipientDeviceId() {
        return recipientDeviceId;
    }

    public int getCipherType() {
        return cipherType;
    }

    public String getCiphertext() {
        return ciphertext;
    }
}
