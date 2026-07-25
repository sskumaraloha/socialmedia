package com.socialmedia.message.websocket;

import com.socialmedia.common.security.JwtValidator;
import com.socialmedia.common.websocket.JwtStompChannelInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * A single-node in-memory STOMP broker for message delivery (new message, typing
 * indicator, receipts). Same caveat as chat-service's WebSocketConfig: this does not fan
 * out across horizontally-scaled message-service instances - that's the platform-wide
 * WebSocket-scaling architecture (task #10).
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtValidator jwtValidator;

    public WebSocketConfig(JwtValidator jwtValidator) {
        this.jwtValidator = jwtValidator;
    }

    @Bean
    public JwtStompChannelInterceptor jwtStompChannelInterceptor() {
        return new JwtStompChannelInterceptor(jwtValidator);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/messages").setAllowedOriginPatterns("*").withSockJS();
        registry.addEndpoint("/ws/messages").setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtStompChannelInterceptor());
    }
}
