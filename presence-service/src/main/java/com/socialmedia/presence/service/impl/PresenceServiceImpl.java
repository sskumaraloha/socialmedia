package com.socialmedia.presence.service.impl;

import com.socialmedia.presence.dto.response.PresenceResponse;
import com.socialmedia.presence.event.PresenceEventPublisher;
import com.socialmedia.presence.service.PresenceService;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Online/offline is tracked as a Redis sorted set (member = userId, score = last
 * heartbeat epoch-millis) rather than per-user TTL keys with Redis keyspace
 * notifications: a scheduled sweep over the sorted set works against any Redis
 * deployment (including managed ones that disallow CONFIG SET for keyspace events),
 * at the cost of detecting a dropped connection within one sweep interval instead of
 * instantly.
 */
@Service
public class PresenceServiceImpl implements PresenceService {

    private static final Logger log = LoggerFactory.getLogger(PresenceServiceImpl.class);
    private static final String HEARTBEATS_KEY = "presence:heartbeats";
    private static final String LAST_SEEN_KEY_PREFIX = "presence:lastseen:";

    private final StringRedisTemplate redisTemplate;
    private final PresenceEventPublisher eventPublisher;
    private final long timeoutMillis;

    public PresenceServiceImpl(StringRedisTemplate redisTemplate, PresenceEventPublisher eventPublisher,
            @Value("${app.presence.timeout-seconds:60}") long timeoutSeconds) {
        this.redisTemplate = redisTemplate;
        this.eventPublisher = eventPublisher;
        this.timeoutMillis = timeoutSeconds * 1000;
    }

    @Override
    public void heartbeat(UUID userId) {
        boolean wasOnline = isOnline(userId);
        Instant now = Instant.now();
        redisTemplate.opsForZSet().add(HEARTBEATS_KEY, userId.toString(), now.toEpochMilli());

        if (!wasOnline) {
            eventPublisher.publishPresenceChanged(userId, true, now);
        }
    }

    @Override
    public void goOffline(UUID userId) {
        redisTemplate.opsForZSet().remove(HEARTBEATS_KEY, userId.toString());
        Instant now = Instant.now();
        redisTemplate.opsForValue().set(LAST_SEEN_KEY_PREFIX + userId, now.toString());
        eventPublisher.publishPresenceChanged(userId, false, now);
    }

    @Override
    public PresenceResponse getPresence(UUID userId) {
        boolean online = isOnline(userId);
        Instant lastSeenAt = lastSeenOf(userId);
        return new PresenceResponse(userId, online, lastSeenAt);
    }

    @Override
    public List<PresenceResponse> getPresence(List<UUID> userIds) {
        return userIds.stream().map(this::getPresence).toList();
    }

    @Override
    public int sweepStaleUsers() {
        long cutoffMillis = Instant.now().toEpochMilli() - timeoutMillis;
        Set<String> stale = redisTemplate.opsForZSet().rangeByScore(HEARTBEATS_KEY, 0, cutoffMillis);
        if (stale == null || stale.isEmpty()) {
            return 0;
        }

        Instant now = Instant.now();
        for (String userIdString : stale) {
            UUID userId = UUID.fromString(userIdString);
            redisTemplate.opsForZSet().remove(HEARTBEATS_KEY, userIdString);
            redisTemplate.opsForValue().set(LAST_SEEN_KEY_PREFIX + userId, now.toString());
            eventPublisher.publishPresenceChanged(userId, false, now);
        }
        log.info("Marked {} stale user(s) offline", stale.size());
        return stale.size();
    }

    private boolean isOnline(UUID userId) {
        Double score = redisTemplate.opsForZSet().score(HEARTBEATS_KEY, userId.toString());
        return score != null && score >= Instant.now().toEpochMilli() - timeoutMillis;
    }

    private Instant lastSeenOf(UUID userId) {
        String value = redisTemplate.opsForValue().get(LAST_SEEN_KEY_PREFIX + userId);
        return value == null ? null : Instant.parse(value);
    }
}
