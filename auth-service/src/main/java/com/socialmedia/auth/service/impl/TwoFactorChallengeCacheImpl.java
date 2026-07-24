package com.socialmedia.auth.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialmedia.auth.security.TokenHasher;
import com.socialmedia.auth.service.TwoFactorChallengeCache;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class TwoFactorChallengeCacheImpl implements TwoFactorChallengeCache {

    private static final Logger log = LoggerFactory.getLogger(TwoFactorChallengeCacheImpl.class);
    private static final String KEY_PREFIX = "auth:2fa-challenge:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public TwoFactorChallengeCacheImpl(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public String create(Challenge challenge, Duration ttl) {
        String token = TokenHasher.generateRawToken();
        try {
            String json = objectMapper.writeValueAsString(challenge);
            redisTemplate.opsForValue().set(KEY_PREFIX + token, json, ttl);
        } catch (Exception e) {
            log.warn("Failed to cache 2FA challenge: {}", e.getMessage());
        }
        return token;
    }

    @Override
    public Optional<Challenge> get(String challengeToken) {
        String json = redisTemplate.opsForValue().get(KEY_PREFIX + challengeToken);
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, Challenge.class));
        } catch (Exception e) {
            log.warn("Failed to deserialize 2FA challenge: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void evict(String challengeToken) {
        redisTemplate.delete(KEY_PREFIX + challengeToken);
    }
}
