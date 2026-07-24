package com.socialmedia.auth.service;

import com.socialmedia.auth.domain.DeviceType;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Redis-backed holding pen between "password verified" and "TOTP code verified" - the
 * client never sees the userId, only an opaque challenge token, so a login attempt that
 * never completes 2FA leaves no way to enumerate accounts.
 */
public interface TwoFactorChallengeCache {

    record Challenge(UUID userId, String deviceId, String deviceName, DeviceType deviceType, String ipAddress,
            String userAgent) {
    }

    String create(Challenge challenge, Duration ttl);

    Optional<Challenge> get(String challengeToken);

    void evict(String challengeToken);
}
