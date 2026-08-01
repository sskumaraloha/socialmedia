package com.socialmedia.message.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** See EncryptedEnvelope for why a single message carries one of these per recipient device. */
public record EncryptedEnvelopeRequest(
        @NotBlank String recipientDeviceId,
        @NotNull Integer cipherType,
        @NotBlank String ciphertext
) {
}
