package com.socialmedia.message.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** An edit re-encrypts the new text client-side for every recipient device, exactly like an
 * original send - the server has no key with which to re-encrypt an edited body itself. */
public record EditMessageRequest(@NotEmpty List<@Valid EncryptedEnvelopeRequest> envelopes) {
}
