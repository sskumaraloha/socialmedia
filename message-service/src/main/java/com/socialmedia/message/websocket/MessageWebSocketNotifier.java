package com.socialmedia.message.websocket;

import com.socialmedia.common.websocket.RedisWebSocketRelay;
import java.util.Collection;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Publishes through the Redis relay rather than calling SimpMessagingTemplate directly -
 * a direct call only reaches a session connected to this exact node, which breaks the
 * moment message-service runs more than one replica (see RedisWebSocketRelay's javadoc).
 */
@Component
public class MessageWebSocketNotifier {

    private static final String USER_DESTINATION = "/queue/message-events";

    private final RedisWebSocketRelay relay;

    public MessageWebSocketNotifier(RedisWebSocketRelay relay) {
        this.relay = relay;
    }

    public void notifyUser(UUID userId, String type, Object payload) {
        relay.publish(userId.toString(), USER_DESTINATION, new MessageWsNotification(type, payload));
    }

    public void notifyUsers(Collection<UUID> userIds, String type, Object payload) {
        userIds.forEach(userId -> notifyUser(userId, type, payload));
    }
}
