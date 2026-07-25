package com.socialmedia.message.websocket;

import com.socialmedia.message.domain.ChatMembership;
import com.socialmedia.message.repository.ChatMembershipRepository;
import java.security.Principal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

/**
 * Typing indicators are pure real-time signal - never persisted, fanned out via the
 * local ChatMembership mirror (kept current from chat-service's Kafka events) so a
 * keystroke-driven event never makes a synchronous call to chat-service.
 */
@Controller
public class TypingController {

    private final ChatMembershipRepository chatMembershipRepository;
    private final MessageWebSocketNotifier notifier;

    public TypingController(ChatMembershipRepository chatMembershipRepository, MessageWebSocketNotifier notifier) {
        this.chatMembershipRepository = chatMembershipRepository;
        this.notifier = notifier;
    }

    @MessageMapping("/chats/{chatId}/typing")
    public void onTyping(@DestinationVariable UUID chatId, Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        TypingEvent event = new TypingEvent(chatId, userId, Instant.now());
        chatMembershipRepository.findAllByChatId(chatId).stream()
                .map(ChatMembership::getUserId)
                .filter(id -> !id.equals(userId))
                .forEach(recipient -> notifier.notifyUser(recipient, "TYPING", event));
    }
}
