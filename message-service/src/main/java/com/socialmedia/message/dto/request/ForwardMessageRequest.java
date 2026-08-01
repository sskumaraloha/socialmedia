package com.socialmedia.message.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/**
 * Forwarding now requires the client to supply freshly-encrypted envelopes for the target
 * chat's devices. Previously the server could just copy the stored ciphertext across, because
 * one global server-side key made every ciphertext readable by every recipient. Under real
 * per-session Double Ratchet keys that shortcut is invalid - ciphertext encrypted for chat A's
 * devices is undecryptable by chat B's - so the forwarding client must decrypt locally and
 * re-encrypt for the new recipients, which is exactly what WhatsApp/Signal clients do.
 */
public record ForwardMessageRequest(
        @NotNull UUID targetChatId,
        @NotEmpty List<@Valid EncryptedEnvelopeRequest> envelopes
) {
}
