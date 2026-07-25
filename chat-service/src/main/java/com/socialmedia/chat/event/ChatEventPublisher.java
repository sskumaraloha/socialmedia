package com.socialmedia.chat.event;

import com.socialmedia.chat.domain.Chat;
import com.socialmedia.chat.event.outgoing.ChatCreatedEvent;
import com.socialmedia.chat.event.outgoing.ChatDeletedEvent;
import com.socialmedia.chat.event.outgoing.ChatMemberAddedEvent;
import com.socialmedia.chat.event.outgoing.ChatMemberRemovedEvent;
import com.socialmedia.chat.event.outgoing.ChatUpdatedEvent;
import com.socialmedia.chat.event.outgoing.MessagePinnedEvent;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ChatEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ChatEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ChatEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishChatCreated(Chat chat, Set<UUID> memberIds) {
        send(ChatTopics.CHAT_CREATED, chat.getId().toString(),
                new ChatCreatedEvent(chat.getId(), chat.getType(), chat.getCreatedBy(), memberIds, Instant.now()));
    }

    public void publishChatUpdated(Chat chat) {
        send(ChatTopics.CHAT_UPDATED, chat.getId().toString(),
                new ChatUpdatedEvent(chat.getId(), chat.getName(), chat.getDescription(), chat.getAvatarUrl(), Instant.now()));
    }

    public void publishMemberAdded(UUID chatId, UUID userId, UUID addedBy) {
        send(ChatTopics.CHAT_MEMBER_ADDED, chatId.toString(),
                new ChatMemberAddedEvent(chatId, userId, addedBy, Instant.now()));
    }

    public void publishMemberRemoved(UUID chatId, UUID userId, UUID removedBy) {
        send(ChatTopics.CHAT_MEMBER_REMOVED, chatId.toString(),
                new ChatMemberRemovedEvent(chatId, userId, removedBy, Instant.now()));
    }

    public void publishChatDeleted(UUID chatId, UUID deletedBy) {
        send(ChatTopics.CHAT_DELETED, chatId.toString(), new ChatDeletedEvent(chatId, deletedBy, Instant.now()));
    }

    public void publishMessagePinned(UUID chatId, UUID messageId, UUID pinnedBy) {
        send(ChatTopics.CHAT_MESSAGE_PINNED, chatId.toString(),
                new MessagePinnedEvent(chatId, messageId, pinnedBy, Instant.now()));
    }

    private void send(String topic, String key, Object payload) {
        try {
            kafkaTemplate.send(topic, key, payload).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.warn("Failed to publish event to topic {} (key={}): {}", topic, key, ex.getMessage());
                }
            });
        } catch (Exception ex) {
            log.warn("Failed to publish event to topic {} (key={}): {}", topic, key, ex.getMessage());
        }
    }
}
