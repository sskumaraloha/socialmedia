package com.socialmedia.auth.dto.response;

public record TwoFactorChallengeResponse(
        String challengeToken,
        String message,
        long expiresInSeconds
) {
}
