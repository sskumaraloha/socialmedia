package com.socialmedia.presence.event;

import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PresenceEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PresenceEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PresenceEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishPresenceChanged(UUID userId, boolean online, Instant lastSeenAt) {
        try {
            kafkaTemplate.send(PresenceTopics.PRESENCE_CHANGED, userId.toString(),
                            new PresenceChangedEvent(userId, online, lastSeenAt))
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("Failed to publish presence.changed.v1 for user {}: {}", userId, ex.getMessage());
                        }
                    });
        } catch (Exception ex) {
            log.warn("Failed to publish presence.changed.v1 for user {}: {}", userId, ex.getMessage());
        }
    }
}
