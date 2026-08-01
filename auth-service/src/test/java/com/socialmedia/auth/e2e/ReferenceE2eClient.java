package com.socialmedia.auth.e2e;

import com.socialmedia.auth.dto.request.UploadKeyBundleRequest;
import com.socialmedia.auth.dto.response.KeyBundleResponse;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.signal.libsignal.protocol.IdentityKey;
import org.signal.libsignal.protocol.IdentityKeyPair;
import org.signal.libsignal.protocol.SessionBuilder;
import org.signal.libsignal.protocol.SessionCipher;
import org.signal.libsignal.protocol.SignalProtocolAddress;
import org.signal.libsignal.protocol.ecc.ECKeyPair;
import org.signal.libsignal.protocol.ecc.ECPublicKey;
import org.signal.libsignal.protocol.kem.KEMKeyPair;
import org.signal.libsignal.protocol.kem.KEMKeyType;
import org.signal.libsignal.protocol.kem.KEMPublicKey;
import org.signal.libsignal.protocol.message.CiphertextMessage;
import org.signal.libsignal.protocol.message.PreKeySignalMessage;
import org.signal.libsignal.protocol.message.SignalMessage;
import org.signal.libsignal.protocol.state.KyberPreKeyRecord;
import org.signal.libsignal.protocol.state.PreKeyBundle;
import org.signal.libsignal.protocol.state.PreKeyRecord;
import org.signal.libsignal.protocol.state.SignedPreKeyRecord;
import org.signal.libsignal.protocol.state.impl.InMemorySignalProtocolStore;

/**
 * A TEST-ONLY stand-in for what a real mobile/web client's crypto layer does. It exists to prove
 * the server-side pieces (auth-service's public key-bundle directory and message-service's opaque
 * ciphertext relay) are genuinely sufficient for real end-to-end encryption - not to ship as
 * production code.
 *
 * <p>Everything secret lives here, in this object, never on the server: the identity private key,
 * the prekey private keys, and the ratcheting session state (held in libsignal's own
 * {@link InMemorySignalProtocolStore}, which is what a real client would back with encrypted
 * on-device storage). The only things this class ever hands to the server are public key bytes
 * and ciphertext.
 */
public class ReferenceE2eClient {

    /** libsignal addresses a peer by (name, intDeviceId); this platform's own device ids are
     * opaque strings, so the reference client keeps its own stable int alongside. */
    private static final int LIBSIGNAL_DEVICE_ID = 1;

    private final String platformDeviceId;
    private final int registrationId;
    private final IdentityKeyPair identityKeyPair;
    private final InMemorySignalProtocolStore store;

    private final int signedPreKeyId = 5;
    private final ECKeyPair signedPreKeyPair;
    private final byte[] signedPreKeySignature;

    private final int kyberPreKeyId = 7;
    private final KEMKeyPair kyberPreKeyPair;
    private final byte[] kyberPreKeySignature;

    private final List<PreKeyRecord> oneTimePreKeys = new ArrayList<>();

    public ReferenceE2eClient(String platformDeviceId, int registrationId, int oneTimePreKeyCount) {
        this.platformDeviceId = platformDeviceId;
        this.registrationId = registrationId;
        this.identityKeyPair = IdentityKeyPair.generate();
        this.store = new InMemorySignalProtocolStore(identityKeyPair, registrationId);

        this.signedPreKeyPair = ECKeyPair.generate();
        this.signedPreKeySignature = identityKeyPair.getPrivateKey()
                .calculateSignature(signedPreKeyPair.getPublicKey().serialize());
        store.storeSignedPreKey(signedPreKeyId,
                new SignedPreKeyRecord(signedPreKeyId, System.currentTimeMillis(), signedPreKeyPair, signedPreKeySignature));

        this.kyberPreKeyPair = KEMKeyPair.generate(KEMKeyType.KYBER_1024);
        this.kyberPreKeySignature = identityKeyPair.getPrivateKey()
                .calculateSignature(kyberPreKeyPair.getPublicKey().serialize());
        store.storeKyberPreKey(kyberPreKeyId,
                new KyberPreKeyRecord(kyberPreKeyId, System.currentTimeMillis(), kyberPreKeyPair, kyberPreKeySignature));

        for (int i = 1; i <= oneTimePreKeyCount; i++) {
            PreKeyRecord record = new PreKeyRecord(i, ECKeyPair.generate());
            oneTimePreKeys.add(record);
            store.storePreKey(i, record);
        }
    }

    /** Exactly what a real client PUTs to /api/v1/auth/devices/{deviceId}/keys - public halves only. */
    public UploadKeyBundleRequest toUploadRequest() {
        List<UploadKeyBundleRequest.OneTimePreKeyUpload> uploads = oneTimePreKeys.stream()
                .map(record -> {
                    try {
                        return new UploadKeyBundleRequest.OneTimePreKeyUpload(record.getId(),
                                b64(record.getKeyPair().getPublicKey().serialize()));
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                })
                .toList();
        return new UploadKeyBundleRequest(registrationId, b64(identityKeyPair.getPublicKey().serialize()),
                signedPreKeyId, b64(signedPreKeyPair.getPublicKey().serialize()), b64(signedPreKeySignature),
                kyberPreKeyId, b64(kyberPreKeyPair.getPublicKey().serialize()), b64(kyberPreKeySignature),
                uploads);
    }

    /**
     * Turns a bundle fetched from the server back into a libsignal PreKeyBundle and runs PQXDH to
     * establish an outbound session. If the server's response is missing anything a real client
     * needs, this is where it fails - which is the point of testing it this way.
     */
    public void establishSessionWith(String peerName, KeyBundleResponse bundle) throws Exception {
        ECPublicKey oneTimePreKey = bundle.oneTimePreKeyPublic() == null
                ? null : new ECPublicKey(unb64(bundle.oneTimePreKeyPublic()));
        int oneTimePreKeyId = bundle.oneTimePreKeyId() == null
                ? PreKeyBundle.NULL_PRE_KEY_ID : bundle.oneTimePreKeyId();

        PreKeyBundle preKeyBundle = new PreKeyBundle(
                bundle.registrationId(),
                LIBSIGNAL_DEVICE_ID,
                oneTimePreKeyId,
                oneTimePreKey,
                bundle.signedPreKeyId(),
                new ECPublicKey(unb64(bundle.signedPreKeyPublic())),
                unb64(bundle.signedPreKeySignature()),
                new IdentityKey(unb64(bundle.identityPublicKey())),
                bundle.kyberPreKeyId(),
                new KEMPublicKey(unb64(bundle.kyberPreKeyPublic())),
                unb64(bundle.kyberPreKeySignature()));

        new SessionBuilder(store, addressOf(peerName)).process(preKeyBundle);
    }

    /** Returns {cipherType, base64Ciphertext} - precisely the two fields message-service stores
     * per recipient device in an EncryptedEnvelope, and all it ever learns about the message. */
    public EncryptedPayload encryptTo(String peerName, String plaintext) throws Exception {
        CiphertextMessage message = new SessionCipher(store, addressOf(peerName))
                .encrypt(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return new EncryptedPayload(message.getType(), b64(message.serialize()));
    }

    public String decryptFrom(String peerName, EncryptedPayload payload) throws Exception {
        SessionCipher cipher = new SessionCipher(store, addressOf(peerName));
        byte[] raw = unb64(payload.ciphertext());
        byte[] plaintext = payload.cipherType() == CiphertextMessage.PREKEY_TYPE
                ? cipher.decrypt(new PreKeySignalMessage(raw))
                : cipher.decrypt(new SignalMessage(raw));
        return new String(plaintext, java.nio.charset.StandardCharsets.UTF_8);
    }

    public String platformDeviceId() {
        return platformDeviceId;
    }

    private static SignalProtocolAddress addressOf(String peerName) {
        return new SignalProtocolAddress(peerName, LIBSIGNAL_DEVICE_ID);
    }

    private static String b64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static byte[] unb64(String value) {
        return Base64.getDecoder().decode(value);
    }

    public record EncryptedPayload(int cipherType, String ciphertext) {
    }
}
