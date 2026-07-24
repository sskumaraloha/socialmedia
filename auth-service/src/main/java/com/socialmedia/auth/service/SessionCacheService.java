package com.socialmedia.auth.service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Redis-backed shadow of the active session table. JwtAuthenticationFilter checks this
 * on every request so a revoked session (logout, "log out all devices", admin action)
 * takes effect immediately instead of waiting for the JWT to naturally expire.
 */
public interface SessionCacheService {

    record CachedSession(UUID userId, String deviceId) {
    }

    void put(String jti, UUID userId, String deviceId, Duration ttl);

    Optional<CachedSession> get(String jti);

    void evict(String jti);

    void touch(String jti, Duration ttl);
}
