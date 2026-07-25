package com.socialmedia.chat.websocket;

import com.socialmedia.common.websocket.RedisWebSocketRelay;
import java.util.Collection;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Publishes through the Redis relay rather than calling SimpMessagingTemplate directly -
 * see RedisWebSocketRelay's javadoc for why: a direct call only reaches a session
 * connected to this exact node, which breaks the moment chat-service runs more than one
 * replica.
 */
@Component
public class ChatWebSocketNotifier {

    private static final String USER_DESTINATION = "/queue/chat-events";

    private final RedisWebSocketRelay relay;

    public ChatWebSocketNotifier(RedisWebSocketRelay relay) {
        this.relay = relay;
    }

    public void notifyUser(UUID userId, String type, Object payload) {
        relay.publish(userId.toString(), USER_DESTINATION, new ChatWsNotification(type, payload));
    }

    public void notifyUsers(Collection<UUID> userIds, String type, Object payload) {
        userIds.forEach(userId -> notifyUser(userId, type, payload));
    }
}
