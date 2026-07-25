package com.socialmedia.common.websocket;

import com.socialmedia.common.security.AuthenticatedPrincipal;
import com.socialmedia.common.security.JwtValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.StringUtils;

/**
 * The STOMP handshake itself is a plain HTTP upgrade with no Authorization header support
 * in most browser WebSocket clients, so authentication happens on the STOMP CONNECT frame
 * instead: the client sends "Authorization: Bearer <token>" as a STOMP native header, and
 * this interceptor validates it before the session is accepted. Register it on any
 * service's own WebSocketConfig - this library intentionally does not auto-wire a full
 * WebSocketMessageBrokerConfigurer, since STOMP endpoint paths differ per service.
 */
public class JwtStompChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtStompChannelInterceptor.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtValidator jwtValidator;

    public JwtStompChannelInterceptor(JwtValidator jwtValidator) {
        this.jwtValidator = jwtValidator;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String header = accessor.getFirstNativeHeader("Authorization");
            if (!StringUtils.hasText(header) || !header.startsWith(BEARER_PREFIX)) {
                throw new MessagingException("Missing bearer token on STOMP CONNECT");
            }
            try {
                AuthenticatedPrincipal principal = jwtValidator.validate(header.substring(BEARER_PREFIX.length()));
                accessor.setUser(new StompUserPrincipal(principal.userId()));
            } catch (Exception e) {
                log.debug("Rejecting STOMP CONNECT with invalid bearer token: {}", e.getMessage());
                throw new MessagingException("Invalid bearer token", e);
            }
        }
        return message;
    }
}
