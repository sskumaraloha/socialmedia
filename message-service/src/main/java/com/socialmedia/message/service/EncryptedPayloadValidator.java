package com.socialmedia.message.service;

import com.socialmedia.common.exception.BusinessException;
import com.socialmedia.message.domain.EncryptedEnvelope;
import com.socialmedia.message.dto.request.EncryptedEnvelopeRequest;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Replaces the old MessageEncryptionService, which held a single server-side AES key and could
 * decrypt every message - explicitly "not end-to-end" by its own javadoc. Now that clients do
 * real Signal-protocol (X3DH + Double Ratchet) encryption per recipient device, this service's
 * entire job is the opposite: confirm an inbound ciphertext blob is well-formed enough to store
 * and relay, while being structurally incapable of reading it. There is no key here, and no
 * decrypt method - that is the point.
 */
@Component
public class EncryptedPayloadValidator {

    /** libsignal's own discriminators: 2 = SignalMessage, 3 = PreKeySignalMessage. */
    private static final Set<Integer> ALLOWED_CIPHER_TYPES = Set.of(2, 3);

    private final int maxCiphertextBytes;
    private final int maxEnvelopesPerMessage;

    public EncryptedPayloadValidator(
            @Value("${app.security.e2e.max-ciphertext-bytes:65536}") int maxCiphertextBytes,
            @Value("${app.security.e2e.max-envelopes-per-message:64}") int maxEnvelopesPerMessage) {
        this.maxCiphertextBytes = maxCiphertextBytes;
        this.maxEnvelopesPerMessage = maxEnvelopesPerMessage;
    }

    public List<EncryptedEnvelope> validateAndConvert(List<EncryptedEnvelopeRequest> requested) {
        if (requested == null || requested.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "NO_ENVELOPES",
                    "At least one encrypted envelope is required - the server cannot encrypt on your behalf");
        }
        if (requested.size() > maxEnvelopesPerMessage) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "TOO_MANY_ENVELOPES",
                    "A message may target at most " + maxEnvelopesPerMessage + " devices");
        }

        Set<String> seenDeviceIds = new HashSet<>();
        List<EncryptedEnvelope> envelopes = new java.util.ArrayList<>(requested.size());
        for (EncryptedEnvelopeRequest envelope : requested) {
            if (!seenDeviceIds.add(envelope.recipientDeviceId())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "DUPLICATE_ENVELOPE_DEVICE",
                        "Two envelopes target the same device: " + envelope.recipientDeviceId());
            }
            if (!ALLOWED_CIPHER_TYPES.contains(envelope.cipherType())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_CIPHER_TYPE",
                        "Unsupported cipher type: " + envelope.cipherType());
            }
            byte[] decoded;
            try {
                decoded = Base64.getDecoder().decode(envelope.ciphertext());
            } catch (IllegalArgumentException e) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_CIPHERTEXT_ENCODING",
                        "Ciphertext must be valid Base64");
            }
            if (decoded.length == 0) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "EMPTY_CIPHERTEXT", "Ciphertext must not be empty");
            }
            if (decoded.length > maxCiphertextBytes) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "CIPHERTEXT_TOO_LARGE",
                        "Ciphertext exceeds the maximum of " + maxCiphertextBytes + " bytes");
            }
            envelopes.add(new EncryptedEnvelope(envelope.recipientDeviceId(), envelope.cipherType(), envelope.ciphertext()));
        }
        return envelopes;
    }
}
