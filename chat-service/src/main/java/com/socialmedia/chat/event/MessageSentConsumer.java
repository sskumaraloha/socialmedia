package com.socialmedia.chat.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialmedia.chat.event.incoming.MessageSentEvent;
import com.socialmedia.chat.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class MessageSentConsumer {

    private static final Logger log = LoggerFactory.getLogger(MessageSentConsumer.class);

    private final ObjectMapper objectMapper;
    private final ChatService chatService;

    public MessageSentConsumer(ObjectMapper objectMapper, ChatService chatService) {
        this.objectMapper = objectMapper;
        this.chatService = chatService;
    }

    @KafkaListener(topics = ChatTopics.MESSAGE_SENT, groupId = "${spring.kafka.consumer.group-id}")
    public void onMessageSent(String payload) {
        try {
            MessageSentEvent event = objectMapper.readValue(payload, MessageSentEvent.class);
            chatService.recordIncomingMessage(event.chatId(), event.senderId(), event.contentPreview(), event.sentAt());
        } catch (Exception e) {
            log.error("Failed to process message.sent.v1 payload: {}", e.getMessage(), e);
        }
    }
}
