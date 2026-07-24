package com.socialmedia.auth.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialmedia.auth.service.SessionCacheService;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Plain JSON strings via StringRedisTemplate rather than RedisTemplate&lt;String,Object&gt;
 * + Jackson polymorphic typing - every read here always expects the exact same concrete
 * type it wrote, so there's no real polymorphism to support, only a class of bugs
 * (WRAPPER_ARRAY vs PROPERTY type-info mismatches) to avoid by not reaching for it.
 */
@Service
public class SessionCacheServiceImpl implements SessionCacheService {

    private static final Logger log = LoggerFactory.getLogger(SessionCacheServiceImpl.class);
    private static final String KEY_PREFIX = "auth:session:";

    private record CachedSessionRecord(String userId, String deviceId) {
    }

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public SessionCacheServiceImpl(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void put(String jti, UUID userId, String deviceId, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(new CachedSessionRecord(userId.toString(), deviceId));
            redisTemplate.opsForValue().set(key(jti), json, ttl);
        } catch (Exception e) {
            log.warn("Failed to cache session {}: {}", jti, e.getMessage());
        }
    }

    @Override
    public Optional<CachedSession> get(String jti) {
        String json = redisTemplate.opsForValue().get(key(jti));
        if (json == null) {
            return Optional.empty();
        }
        try {
            CachedSessionRecord record = objectMapper.readValue(json, CachedSessionRecord.class);
            return Optional.of(new CachedSession(UUID.fromString(record.userId()), record.deviceId()));
        } catch (Exception e) {
            log.warn("Failed to deserialize cached session {}: {}", jti, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void evict(String jti) {
        redisTemplate.delete(key(jti));
    }

    @Override
    public void touch(String jti, Duration ttl) {
        redisTemplate.expire(key(jti), ttl);
    }

    private String key(String jti) {
        return KEY_PREFIX + jti;
    }
}
