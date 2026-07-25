package com.socialmedia.message.websocket;

import java.util.Collection;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class MessageWebSocketNotifier {

    private static final Logger log = LoggerFactory.getLogger(MessageWebSocketNotifier.class);
    private static final String USER_DESTINATION = "/queue/message-events";

    private final SimpMessagingTemplate messagingTemplate;

    public MessageWebSocketNotifier(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void notifyUser(UUID userId, String type, Object payload) {
        try {
            messagingTemplate.convertAndSendToUser(userId.toString(), USER_DESTINATION, new MessageWsNotification(type, payload));
        } catch (Exception e) {
            log.debug("Failed to deliver ws notification {} to user {}: {}", type, userId, e.getMessage());
        }
    }

    public void notifyUsers(Collection<UUID> userIds, String type, Object payload) {
        userIds.forEach(userId -> notifyUser(userId, type, payload));
    }
}
