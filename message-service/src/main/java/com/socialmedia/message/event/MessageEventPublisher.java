package com.socialmedia.message.event;

import com.socialmedia.message.domain.Message;
import com.socialmedia.message.event.outgoing.MessageDeletedEvent;
import com.socialmedia.message.event.outgoing.MessageSentEvent;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class MessageEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(MessageEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public MessageEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /** Metadata only - see MessageSentEvent for why no content preview can be published. */
    public void publishMessageSent(Message message) {
        send(MessageTopics.MESSAGE_SENT, message.getChatId().toString(),
                new MessageSentEvent(message.getId(), message.getChatId(), message.getSenderId(), message.getCreatedAt()));
    }

    public void publishMessageDeleted(UUID messageId, UUID chatId, UUID deletedBy) {
        send(MessageTopics.MESSAGE_DELETED, chatId.toString(),
                new MessageDeletedEvent(messageId, chatId, deletedBy, Instant.now()));
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
