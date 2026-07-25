package com.socialmedia.search.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialmedia.search.domain.ChatDocument;
import com.socialmedia.search.event.incoming.ChatCreatedEvent;
import com.socialmedia.search.event.incoming.ChatDeletedEvent;
import com.socialmedia.search.event.incoming.ChatUpdatedEvent;
import com.socialmedia.search.service.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Only GROUP/CHANNEL chats are indexed - a PRIVATE chat has no name to search by, and
 * chat-service itself refuses name/description updates on a PRIVATE chat, so a
 * chat.updated.v1 event is guaranteed to only ever reference an already-indexed chat. */
@Component
public class ChatEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ChatEventConsumer.class);
    private static final String PRIVATE = "PRIVATE";

    private final ObjectMapper objectMapper;
    private final SearchService searchService;

    public ChatEventConsumer(ObjectMapper objectMapper, SearchService searchService) {
        this.objectMapper = objectMapper;
        this.searchService = searchService;
    }

    @KafkaListener(topics = SearchTopics.CHAT_CREATED, groupId = "${spring.kafka.consumer.group-id}")
    public void onChatCreated(String payload) {
        try {
            ChatCreatedEvent event = objectMapper.readValue(payload, ChatCreatedEvent.class);
            if (PRIVATE.equals(event.type())) {
                return;
            }
            searchService.indexChat(new ChatDocument(event.chatId(), event.type(), event.name(), null));
        } catch (Exception e) {
            log.error("Failed to process chat.created.v1 payload: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = SearchTopics.CHAT_UPDATED, groupId = "${spring.kafka.consumer.group-id}")
    public void onChatUpdated(String payload) {
        try {
            ChatUpdatedEvent event = objectMapper.readValue(payload, ChatUpdatedEvent.class);
            searchService.updateChatMeta(event.chatId(), event.name(), event.description());
        } catch (Exception e) {
            log.error("Failed to process chat.updated.v1 payload: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = SearchTopics.CHAT_DELETED, groupId = "${spring.kafka.consumer.group-id}")
    public void onChatDeleted(String payload) {
        try {
            ChatDeletedEvent event = objectMapper.readValue(payload, ChatDeletedEvent.class);
            searchService.deleteChat(event.chatId());
        } catch (Exception e) {
            log.error("Failed to process chat.deleted.v1 payload: {}", e.getMessage(), e);
        }
    }
}
