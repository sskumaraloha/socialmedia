package com.socialmedia.common.lock;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * A cross-instance mutual-exclusion lock over Redis (`SET key value NX PX` to acquire, a
 * compare-and-delete Lua script to release), for critical sections that a single JVM's
 * in-memory lock can't protect once a service is horizontally scaled - e.g. two replicas of
 * the same service racing on the same business key. Not auto-wired (same convention as
 * {@link com.socialmedia.common.websocket.RedisWebSocketRelay}): only services that already
 * depend on Redis for something else should register this as a bean, via their own config.
 *
 * <p>The release script only deletes the key if its value still matches the token this
 * instance set - without that check, a lock whose lease expired mid-critical-section could
 * be released by its rightful owner AFTER a different caller had already acquired it in the
 * meantime, silently unlocking someone else's in-progress work.
 */
public class RedisDistributedLock {

    private static final Logger log = LoggerFactory.getLogger(RedisDistributedLock.class);

    private static final String KEY_PREFIX = "lock:";

    private static final RedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('del', KEYS[1]) "
                    + "else return 0 end",
            Long.class);

    private final StringRedisTemplate redisTemplate;

    public RedisDistributedLock(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** Attempts to acquire the lock exactly once, without waiting. Returns the release token
     * to pass back to {@link #unlock(String, String)} on success, or empty if someone else
     * already holds it. */
    public Optional<String> tryLock(String key, Duration leaseTime) {
        String token = UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + key, token, leaseTime);
        return Boolean.TRUE.equals(acquired) ? Optional.of(token) : Optional.empty();
    }

    public void unlock(String key, String token) {
        try {
            redisTemplate.execute(RELEASE_SCRIPT, List.of(KEY_PREFIX + key), token);
        } catch (Exception e) {
            log.warn("Failed to release distributed lock for key {}: {}", key, e.getMessage());
        }
    }

    /**
     * Runs {@code action} while holding the lock for {@code key}, polling for up to
     * {@code maxWait} before giving up. Guarantees the lock is released afterward even if
     * {@code action} throws. Throws {@link LockAcquisitionException} if the lock is still
     * held by someone else once {@code maxWait} elapses.
     */
    public <T> T withLock(String key, Duration leaseTime, Duration maxWait, Supplier<T> action) {
        Instant deadline = Instant.now().plus(maxWait);
        Optional<String> token = tryLock(key, leaseTime);
        while (token.isEmpty() && Instant.now().isBefore(deadline)) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new LockAcquisitionException(key);
            }
            token = tryLock(key, leaseTime);
        }
        if (token.isEmpty()) {
            throw new LockAcquisitionException(key);
        }
        try {
            return action.get();
        } finally {
            unlock(key, token.get());
        }
    }
}
