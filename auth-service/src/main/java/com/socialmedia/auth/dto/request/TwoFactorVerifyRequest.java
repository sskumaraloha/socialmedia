package com.socialmedia.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TwoFactorVerifyRequest(
        @NotBlank @Pattern(regexp = "\\d{6}") String code
) {
}
