package com.socialmedia.common.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * Delivers a relayed notification to any WebSocket session connected to THIS node -
 * a no-op if the target user isn't connected here (see RedisWebSocketRelay's javadoc).
 */
public class RedisWebSocketRelayListener implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(RedisWebSocketRelayListener.class);

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public RedisWebSocketRelayListener(ObjectMapper objectMapper, SimpMessagingTemplate messagingTemplate) {
        this.objectMapper = objectMapper;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody(), StandardCharsets.UTF_8);
            RelayEnvelope envelope = objectMapper.readValue(json, RelayEnvelope.class);
            messagingTemplate.convertAndSendToUser(envelope.targetUserId(), envelope.destination(), envelope.payload());
        } catch (Exception e) {
            log.warn("Failed to process relayed WebSocket notification: {}", e.getMessage());
        }
    }
}
