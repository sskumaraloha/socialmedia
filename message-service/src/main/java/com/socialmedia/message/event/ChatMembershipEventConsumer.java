package com.socialmedia.message.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialmedia.message.domain.ChatMembership;
import com.socialmedia.message.event.incoming.ChatCreatedEvent;
import com.socialmedia.message.event.incoming.ChatDeletedEvent;
import com.socialmedia.message.event.incoming.ChatMemberAddedEvent;
import com.socialmedia.message.event.incoming.ChatMemberRemovedEvent;
import com.socialmedia.message.repository.ChatMembershipRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Keeps the local, read-only ChatMembership mirror current so typing-indicator / new-
 * message fan-out never needs a synchronous call to chat-service (see ChatMembership's
 * javadoc for why this mirror is NOT used for the authoritative send-authorization check).
 */
@Component
public class ChatMembershipEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ChatMembershipEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final ChatMembershipRepository chatMembershipRepository;

    public ChatMembershipEventConsumer(ObjectMapper objectMapper, ChatMembershipRepository chatMembershipRepository) {
        this.objectMapper = objectMapper;
        this.chatMembershipRepository = chatMembershipRepository;
    }

    @KafkaListener(topics = MessageTopics.CHAT_CREATED, groupId = "${spring.kafka.consumer.group-id}")
    public void onChatCreated(String payload) {
        try {
            ChatCreatedEvent event = objectMapper.readValue(payload, ChatCreatedEvent.class);
            event.memberIds().forEach(userId -> {
                if (!chatMembershipRepository.existsByChatIdAndUserId(event.chatId(), userId)) {
                    chatMembershipRepository.save(new ChatMembership(event.chatId(), userId));
                }
            });
        } catch (Exception e) {
            log.error("Failed to process chat.created.v1 payload: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = MessageTopics.CHAT_MEMBER_ADDED, groupId = "${spring.kafka.consumer.group-id}")
    public void onChatMemberAdded(String payload) {
        try {
            ChatMemberAddedEvent event = objectMapper.readValue(payload, ChatMemberAddedEvent.class);
            if (!chatMembershipRepository.existsByChatIdAndUserId(event.chatId(), event.userId())) {
                chatMembershipRepository.save(new ChatMembership(event.chatId(), event.userId()));
            }
        } catch (Exception e) {
            log.error("Failed to process chat.member.added.v1 payload: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = MessageTopics.CHAT_MEMBER_REMOVED, groupId = "${spring.kafka.consumer.group-id}")
    public void onChatMemberRemoved(String payload) {
        try {
            ChatMemberRemovedEvent event = objectMapper.readValue(payload, ChatMemberRemovedEvent.class);
            chatMembershipRepository.deleteByChatIdAndUserId(event.chatId(), event.userId());
        } catch (Exception e) {
            log.error("Failed to process chat.member.removed.v1 payload: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = MessageTopics.CHAT_DELETED, groupId = "${spring.kafka.consumer.group-id}")
    public void onChatDeleted(String payload) {
        try {
            ChatDeletedEvent event = objectMapper.readValue(payload, ChatDeletedEvent.class);
            chatMembershipRepository.deleteByChatId(event.chatId());
        } catch (Exception e) {
            log.error("Failed to process chat.deleted.v1 payload: {}", e.getMessage(), e);
        }
    }
}
