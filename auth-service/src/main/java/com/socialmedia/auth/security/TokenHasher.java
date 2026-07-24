package com.socialmedia.auth.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Refresh tokens, email-verification tokens, and password-reset tokens are all
 * single-use bearer secrets: we generate a high-entropy random value, hand the raw
 * value to the client/email link, and store only its SHA-256 hash. A DB leak then
 * reveals nothing usable.
 */
public final class TokenHasher {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private TokenHasher() {
    }

    public static String generateRawToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
