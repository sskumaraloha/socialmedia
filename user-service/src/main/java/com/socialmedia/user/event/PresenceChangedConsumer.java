package com.socialmedia.user.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialmedia.user.event.incoming.PresenceChangedEvent;
import com.socialmedia.user.service.UserProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Keeps UserProfile.lastKnownOnlineAt as a best-effort cache of presence-service's live
 * state, so profile reads don't need a synchronous cross-service call. Not built yet as
 * of this writing (see docs/ARCHITECTURE.md) - this consumer simply idles harmlessly
 * against a not-yet-existing topic until presence-service starts publishing to it.
 */
@Component
public class PresenceChangedConsumer {

    private static final Logger log = LoggerFactory.getLogger(PresenceChangedConsumer.class);

    private final ObjectMapper objectMapper;
    private final UserProfileService userProfileService;

    public PresenceChangedConsumer(ObjectMapper objectMapper, UserProfileService userProfileService) {
        this.objectMapper = objectMapper;
        this.userProfileService = userProfileService;
    }

    @KafkaListener(topics = UserTopics.PRESENCE_CHANGED, groupId = "${spring.kafka.consumer.group-id}")
    public void onPresenceChanged(String payload) {
        try {
            PresenceChangedEvent event = objectMapper.readValue(payload, PresenceChangedEvent.class);
            userProfileService.updateLastKnownOnline(event.userId(), event.online(), event.lastSeenAt());
        } catch (Exception e) {
            log.error("Failed to process presence.changed.v1 payload: {}", e.getMessage(), e);
        }
    }
}
