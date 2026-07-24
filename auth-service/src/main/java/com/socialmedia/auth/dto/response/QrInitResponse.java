package com.socialmedia.auth.dto.response;

public record QrInitResponse(
        String qrToken,
        String qrCodeImageBase64,
        long expiresInSeconds
) {
}
