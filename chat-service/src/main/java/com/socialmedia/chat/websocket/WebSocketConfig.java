package com.socialmedia.chat.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * A single-node in-memory STOMP broker for chat-metadata notifications (member added,
 * chat renamed, unread count changed). This does NOT fan out across chat-service
 * instances - horizontally-scaled real-time delivery (Redis pub/sub relay so a client
 * connected to node A sees an event produced on node B) is message-service /
 * presence-service's job (task #10, the platform-wide WebSocket scaling architecture).
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtStompChannelInterceptor jwtStompChannelInterceptor;

    public WebSocketConfig(JwtStompChannelInterceptor jwtStompChannelInterceptor) {
        this.jwtStompChannelInterceptor = jwtStompChannelInterceptor;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/chat").setAllowedOriginPatterns("*").withSockJS();
        registry.addEndpoint("/ws/chat").setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtStompChannelInterceptor);
    }
}
