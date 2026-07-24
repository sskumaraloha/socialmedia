package com.socialmedia.auth.service.impl;

import com.socialmedia.auth.service.RateLimiterService;
import java.time.Duration;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

/**
 * Fixed-window counter implemented as a single atomic Lua script (INCR + conditional
 * PEXPIRE) so concurrent requests from the same key never race past the limit -
 * two round-trips (GET then INCR) would allow exactly that.
 */
@Service
public class RateLimiterServiceImpl implements RateLimiterService {

    private static final String SCRIPT = """
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('PEXPIRE', KEYS[1], ARGV[1])
            end
            return current
            """;

    private static final RedisScript<Long> INCREMENT_SCRIPT = new DefaultRedisScript<>(SCRIPT, Long.class);
    private static final String KEY_PREFIX = "auth:ratelimit:";

    private final StringRedisTemplate redisTemplate;

    public RateLimiterServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean tryConsume(String key, int limit, Duration window) {
        Long current = redisTemplate.execute(INCREMENT_SCRIPT, List.of(KEY_PREFIX + key),
                String.valueOf(window.toMillis()));
        return current != null && current <= limit;
    }
}
