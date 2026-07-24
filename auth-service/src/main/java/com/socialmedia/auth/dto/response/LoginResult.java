package com.socialmedia.auth.dto.response;

/**
 * login() can't just return AuthTokenResponse: a 2FA-enabled account needs to hand the
 * client a challenge token instead. A subclass of BusinessException carrying that token
 * doesn't work here - common-library's GlobalExceptionHandler only renders the generic
 * ApiError shape, which has no field for it, so the token would be silently dropped.
 * A plain result object the controller inspects avoids that trap entirely.
 */
public record LoginResult(AuthTokenResponse tokens, TwoFactorChallengeResponse twoFactorChallenge) {

    public static LoginResult ofTokens(AuthTokenResponse tokens) {
        return new LoginResult(tokens, null);
    }

    public static LoginResult ofChallenge(TwoFactorChallengeResponse challenge) {
        return new LoginResult(null, challenge);
    }

    public boolean requiresTwoFactor() {
        return twoFactorChallenge != null;
    }
}
