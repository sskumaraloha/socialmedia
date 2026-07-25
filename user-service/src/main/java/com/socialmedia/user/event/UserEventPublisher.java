package com.socialmedia.user.event;

import com.socialmedia.user.domain.UserProfile;
import com.socialmedia.user.event.outgoing.UserBlockedEvent;
import com.socialmedia.user.event.outgoing.UserFollowedEvent;
import com.socialmedia.user.event.outgoing.UserProfileCreatedEvent;
import com.socialmedia.user.event.outgoing.UserProfileUpdatedEvent;
import com.socialmedia.user.event.outgoing.UserUnfollowedEvent;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(UserEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public UserEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishProfileCreated(UserProfile profile) {
        send(UserTopics.USER_PROFILE_CREATED, profile.getId().toString(),
                new UserProfileCreatedEvent(profile.getId(), profile.getUsername(), Instant.now()));
    }

    public void publishProfileUpdated(UserProfile profile) {
        send(UserTopics.USER_PROFILE_UPDATED, profile.getId().toString(),
                new UserProfileUpdatedEvent(profile.getId(), profile.getUsername(), profile.getDisplayName(),
                        profile.getBio(), profile.getAvatarUrl(), Instant.now()));
    }

    public void publishFollowed(UUID followerId, UUID followingId) {
        send(UserTopics.USER_FOLLOWED, followingId.toString(),
                new UserFollowedEvent(followerId, followingId, Instant.now()));
    }

    public void publishUnfollowed(UUID followerId, UUID followingId) {
        send(UserTopics.USER_UNFOLLOWED, followingId.toString(),
                new UserUnfollowedEvent(followerId, followingId, Instant.now()));
    }

    public void publishBlocked(UUID blockerId, UUID blockedId) {
        send(UserTopics.USER_BLOCKED, blockedId.toString(), new UserBlockedEvent(blockerId, blockedId, Instant.now()));
    }

    private void send(String topic, String key, Object payload) {
        // producer.send() can throw synchronously (e.g. a metadata-fetch timeout when the
        // broker is unreachable) rather than only ever failing the returned future - both
        // paths must be swallowed here, or a Kafka outage would take down every write path
        // that publishes an event (see auth-service's AuthEventPublisher for how this was found).
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
