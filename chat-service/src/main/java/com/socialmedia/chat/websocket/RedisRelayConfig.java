package com.socialmedia.chat.websocket;

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
 * Wires chat-service into the shared Redis Pub/Sub relay (see common-library's
 * RedisWebSocketRelay) so chat-metadata notifications reach a client regardless of which
 * chat-service instance it's connected to - this is what makes ChatWebSocketNotifier
 * actually horizontally scalable rather than single-node-only.
 */
@Configuration
public class RedisRelayConfig {

    private static final String CHANNEL = "ws:relay:chat-service";

    @Bean
    public RedisWebSocketRelay chatWebSocketRelay(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        return new RedisWebSocketRelay(redisTemplate, objectMapper, CHANNEL);
    }

    @Bean
    public RedisMessageListenerContainer chatWebSocketRelayListenerContainer(RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper, SimpMessagingTemplate messagingTemplate) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(new RedisWebSocketRelayListener(objectMapper, messagingTemplate), new ChannelTopic(CHANNEL));
        return container;
    }
}
