package com.socialmedia.search.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialmedia.search.domain.MessageDocument;
import com.socialmedia.search.event.incoming.MessageDeletedEvent;
import com.socialmedia.search.event.incoming.MessageSentEvent;
import com.socialmedia.search.service.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class MessageEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(MessageEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final SearchService searchService;

    public MessageEventConsumer(ObjectMapper objectMapper, SearchService searchService) {
        this.objectMapper = objectMapper;
        this.searchService = searchService;
    }

    @KafkaListener(topics = SearchTopics.MESSAGE_SENT, groupId = "${spring.kafka.consumer.group-id}")
    public void onMessageSent(String payload) {
        try {
            MessageSentEvent event = objectMapper.readValue(payload, MessageSentEvent.class);
            if (event.contentPreview() == null) {
                return;
            }
            searchService.indexMessage(new MessageDocument(event.messageId(), event.chatId(), event.senderId(),
                    event.contentPreview(), event.sentAt()));
        } catch (Exception e) {
            log.error("Failed to process message.sent.v1 payload: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = SearchTopics.MESSAGE_DELETED, groupId = "${spring.kafka.consumer.group-id}")
    public void onMessageDeleted(String payload) {
        try {
            MessageDeletedEvent event = objectMapper.readValue(payload, MessageDeletedEvent.class);
            searchService.deleteMessage(event.messageId());
        } catch (Exception e) {
            log.error("Failed to process message.deleted.v1 payload: {}", e.getMessage(), e);
        }
    }
}
