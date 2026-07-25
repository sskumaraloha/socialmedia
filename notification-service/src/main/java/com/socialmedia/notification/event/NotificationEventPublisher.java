package com.socialmedia.notification.event;

import com.socialmedia.notification.event.outgoing.NotificationSendRequestEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public NotificationEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void requestSend(NotificationSendRequestEvent event) {
        try {
            kafkaTemplate.send(NotificationTopics.NOTIFICATION_SEND_REQUESTED, event.userId().toString(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("Failed to publish notification.send.v1 for user {}: {}", event.userId(), ex.getMessage());
                        }
                    });
        } catch (Exception ex) {
            log.warn("Failed to publish notification.send.v1 for user {}: {}", event.userId(), ex.getMessage());
        }
    }
}
