package com.socialmedia.auth.dto.response;

public record TwoFactorSetupResponse(
        String secret,
        String otpAuthUrl,
        String qrCodeImageBase64
) {
}
