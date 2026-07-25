package com.socialmedia.chat.websocket;

import java.util.Collection;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class ChatWebSocketNotifier {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketNotifier.class);
    private static final String USER_DESTINATION = "/queue/chat-events";

    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketNotifier(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void notifyUser(UUID userId, String type, Object payload) {
        try {
            messagingTemplate.convertAndSendToUser(userId.toString(), USER_DESTINATION, new ChatWsNotification(type, payload));
        } catch (Exception e) {
            // A disconnected/unregistered session must never fail the REST call that triggered
            // the notification - real-time delivery is best-effort, the REST response is authoritative.
            log.debug("Failed to deliver ws notification {} to user {}: {}", type, userId, e.getMessage());
        }
    }

    public void notifyUsers(Collection<UUID> userIds, String type, Object payload) {
        userIds.forEach(userId -> notifyUser(userId, type, payload));
    }
}
