package com.socialmedia.auth.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;

/**
 * All key fields are Base64-encoded raw public key bytes, as a real client's crypto library
 * (libsignal-client) serializes them. The kyber* fields are required, not optional: the modern
 * Signal protocol is PQXDH (post-quantum hybrid), and a bundle without a Kyber prekey cannot be
 * used to establish a session.
 */
public record UploadKeyBundleRequest(
        @NotNull @PositiveOrZero Integer registrationId,
        @NotBlank String identityPublicKey,
        @NotNull @PositiveOrZero Integer signedPreKeyId,
        @NotBlank String signedPreKeyPublic,
        @NotBlank String signedPreKeySignature,
        @NotNull @PositiveOrZero Integer kyberPreKeyId,
        @NotBlank String kyberPreKeyPublic,
        @NotBlank String kyberPreKeySignature,
        @NotEmpty List<@Valid OneTimePreKeyUpload> oneTimePreKeys
) {

    public record OneTimePreKeyUpload(
            @NotNull @PositiveOrZero Integer keyId,
            @NotBlank String publicKey
    ) {
    }
}
