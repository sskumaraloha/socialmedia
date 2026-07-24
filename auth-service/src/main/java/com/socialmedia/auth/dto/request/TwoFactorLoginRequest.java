package com.socialmedia.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TwoFactorLoginRequest(
        @NotBlank String challengeToken,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "code must be a 6-digit TOTP code") String code
) {
}
