package com.socialmedia.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * A user reporting a message they received. The reporting client attaches the decrypted text
 * itself, because the server has no way to obtain it - this is exactly how abuse reporting works
 * on real end-to-end encrypted platforms: reporting is the moment a recipient voluntarily
 * discloses plaintext they already hold.
 */
public record ReportMessageRequest(
        @NotNull UUID messageId,
        @NotNull UUID chatId,
        @NotNull UUID senderId,
        @NotBlank String decryptedText
) {
}
