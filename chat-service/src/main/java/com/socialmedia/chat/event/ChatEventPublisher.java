package com.socialmedia.chat.event;

import com.socialmedia.chat.domain.Chat;
import com.socialmedia.chat.event.outgoing.ChatCreatedEvent;
import com.socialmedia.chat.event.outgoing.ChatDeletedEvent;
import com.socialmedia.chat.event.outgoing.ChatMemberAddedEvent;
import com.socialmedia.chat.event.outgoing.ChatMemberRemovedEvent;
import com.socialmedia.chat.event.outgoing.ChatUpdatedEvent;
import com.socialmedia.chat.event.outgoing.MessagePinnedEvent;
import com.socialmedia.chat.outbox.OutboxEvent;
import com.socialmedia.chat.outbox.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Stages every outgoing event into the transactional outbox instead of calling Kafka
 * directly - the insert below runs in the SAME transaction as the domain write that
 * triggered it (ChatServiceImpl is class-level @Transactional), so a chat being created/
 * updated/deleted and the fact that an event needs to be published about it either both
 * commit or both roll back together. Unlike the old direct-send version, a failure here
 * is NOT caught and logged: since staging is now just a local database write sharing the
 * caller's transaction, letting it throw and roll back the whole operation is exactly
 * right - the alternative (swallowing it) would silently corrupt the outbox guarantee by
 * committing the domain change without ever queuing its event. See
 * OutboxEventPublisherScheduler for the separate poller that actually delivers these to Kafka.
 */
@Component
public class ChatEventPublisher {

    private final ObjectMapper objectMapper;
    private final OutboxEventRepository outboxEventRepository;

    public ChatEventPublisher(ObjectMapper objectMapper, OutboxEventRepository outboxEventRepository) {
        this.objectMapper = objectMapper;
        this.outboxEventRepository = outboxEventRepository;
    }

    public void publishChatCreated(Chat chat, Set<UUID> memberIds) {
        stage(ChatTopics.CHAT_CREATED, chat.getId().toString(),
                new ChatCreatedEvent(chat.getId(), chat.getType(), chat.getName(), chat.getCreatedBy(), memberIds, Instant.now()));
    }

    public void publishChatUpdated(Chat chat) {
        stage(ChatTopics.CHAT_UPDATED, chat.getId().toString(),
                new ChatUpdatedEvent(chat.getId(), chat.getName(), chat.getDescription(), chat.getAvatarUrl(), Instant.now()));
    }

    public void publishMemberAdded(UUID chatId, UUID userId, UUID addedBy) {
        stage(ChatTopics.CHAT_MEMBER_ADDED, chatId.toString(),
                new ChatMemberAddedEvent(chatId, userId, addedBy, Instant.now()));
    }

    public void publishMemberRemoved(UUID chatId, UUID userId, UUID removedBy) {
        stage(ChatTopics.CHAT_MEMBER_REMOVED, chatId.toString(),
                new ChatMemberRemovedEvent(chatId, userId, removedBy, Instant.now()));
    }

    public void publishChatDeleted(UUID chatId, UUID deletedBy) {
        stage(ChatTopics.CHAT_DELETED, chatId.toString(), new ChatDeletedEvent(chatId, deletedBy, Instant.now()));
    }

    public void publishMessagePinned(UUID chatId, UUID messageId, UUID pinnedBy) {
        stage(ChatTopics.CHAT_MESSAGE_PINNED, chatId.toString(),
                new MessagePinnedEvent(chatId, messageId, pinnedBy, Instant.now()));
    }

    private void stage(String topic, String key, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            outboxEventRepository.save(new OutboxEvent(topic, key, payload.getClass().getSimpleName(), json));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox event for topic " + topic, e);
        }
    }
}
