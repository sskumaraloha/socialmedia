package com.socialmedia.auth.dto.response;

/**
 * Everything a peer's client needs to construct a libsignal {@code PreKeyBundle} and run PQXDH.
 *
 * <p>oneTimePreKeyId/oneTimePreKeyPublic are null when the device's one-time prekey pool is
 * exhausted - a real client still proceeds using only the signed and Kyber prekeys in that case
 * (weaker forward secrecy for just the first message of that session, matching real Signal
 * behavior), rather than refusing to establish a session.
 */
public record KeyBundleResponse(
        String deviceId,
        int registrationId,
        String identityPublicKey,
        int signedPreKeyId,
        String signedPreKeyPublic,
        String signedPreKeySignature,
        int kyberPreKeyId,
        String kyberPreKeyPublic,
        String kyberPreKeySignature,
        Integer oneTimePreKeyId,
        String oneTimePreKeyPublic
) {
}
