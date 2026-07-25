package com.socialmedia.ai.event;

import com.socialmedia.ai.event.outgoing.MessageModeratedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class AiEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(AiEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public AiEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishMessageModerated(MessageModeratedEvent event) {
        try {
            kafkaTemplate.send(AiTopics.MESSAGE_MODERATED, event.messageId().toString(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("Failed to publish ai.message.moderated.v1 for message {}: {}", event.messageId(), ex.getMessage());
                        }
                    });
        } catch (Exception ex) {
            log.warn("Failed to publish ai.message.moderated.v1 for message {}: {}", event.messageId(), ex.getMessage());
        }
    }
}
