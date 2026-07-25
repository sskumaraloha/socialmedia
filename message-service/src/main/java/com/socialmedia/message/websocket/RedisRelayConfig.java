package com.socialmedia.message.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialmedia.common.websocket.RedisWebSocketRelay;
import com.socialmedia.common.websocket.RedisWebSocketRelayListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * Wires message-service into the shared Redis Pub/Sub relay (see common-library's
 * RedisWebSocketRelay) so new-message / typing notifications reach a client regardless
 * of which message-service instance it's connected to.
 */
@Configuration
public class RedisRelayConfig {

    private static final String CHANNEL = "ws:relay:message-service";

    @Bean
    public RedisWebSocketRelay messageWebSocketRelay(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        return new RedisWebSocketRelay(redisTemplate, objectMapper, CHANNEL);
    }

    @Bean
    public RedisMessageListenerContainer messageWebSocketRelayListenerContainer(RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper, SimpMessagingTemplate messagingTemplate) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(new RedisWebSocketRelayListener(objectMapper, messagingTemplate), new ChannelTopic(CHANNEL));
        return container;
    }
}
