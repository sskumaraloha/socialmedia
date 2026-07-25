package com.socialmedia.common.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * Makes user-targeted STOMP delivery work across a horizontally-scaled fleet: a naive
 * SimpMessagingTemplate.convertAndSendToUser() only reaches a session connected to the
 * SAME node that called it, so a message produced on node A never reaches a client
 * connected to node B. This relay instead publishes every notification to a Redis
 * Pub/Sub channel; every node (via RedisWebSocketRelayListener, subscribed on the same
 * channel) receives it and attempts local delivery - a no-op on any node where that user
 * isn't currently connected, and successful delivery on the one node where they are.
 * Register both this bean and the listener via a service's own config (this library
 * intentionally doesn't auto-wire Redis - not every service uses it for this purpose).
 */
public class RedisWebSocketRelay {

    private static final Logger log = LoggerFactory.getLogger(RedisWebSocketRelay.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final String channel;

    public RedisWebSocketRelay(StringRedisTemplate redisTemplate, ObjectMapper objectMapper, String channel) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.channel = channel;
    }

    public void publish(String targetUserId, String destination, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(new RelayEnvelope(targetUserId, destination, payload));
            redisTemplate.convertAndSend(channel, json);
        } catch (Exception e) {
            log.warn("Failed to publish WebSocket relay notification for user {} on channel {}: {}",
                    targetUserId, channel, e.getMessage());
        }
    }

    ObjectMapper objectMapper() {
        return objectMapper;
    }
}
