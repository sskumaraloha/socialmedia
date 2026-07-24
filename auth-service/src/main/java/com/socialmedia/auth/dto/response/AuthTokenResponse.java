package com.socialmedia.auth.dto.response;

public record AuthTokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserSummaryResponse user
) {
    public static AuthTokenResponse bearer(String accessToken, String refreshToken, long expiresIn,
            UserSummaryResponse user) {
        return new AuthTokenResponse(accessToken, refreshToken, "Bearer", expiresIn, user);
    }
}
