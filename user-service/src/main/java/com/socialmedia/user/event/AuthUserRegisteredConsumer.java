package com.socialmedia.user.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialmedia.user.event.incoming.AuthUserRegisteredEvent;
import com.socialmedia.user.service.UserProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Creates a profile row the moment auth-service registers a new account, so user-service
 * never has to synchronously call auth-service (or vice versa) - each service reacts to
 * events from the other asynchronously.
 *
 * Consumed as a plain String (not Spring Kafka's JsonDeserializer): cross-service Kafka
 * payloads shouldn't rely on Java type headers naming the PRODUCER's class, since the
 * consumer has no such class on its classpath - see UserEventPublisher for the producer
 * side of this same principle.
 */
@Component
public class AuthUserRegisteredConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuthUserRegisteredConsumer.class);

    private final ObjectMapper objectMapper;
    private final UserProfileService userProfileService;

    public AuthUserRegisteredConsumer(ObjectMapper objectMapper, UserProfileService userProfileService) {
        this.objectMapper = objectMapper;
        this.userProfileService = userProfileService;
    }

    @KafkaListener(topics = UserTopics.AUTH_USER_REGISTERED, groupId = "${spring.kafka.consumer.group-id}")
    public void onUserRegistered(String payload) {
        try {
            AuthUserRegisteredEvent event = objectMapper.readValue(payload, AuthUserRegisteredEvent.class);
            userProfileService.createProfileForNewUser(event.userId(), event.email());
        } catch (Exception e) {
            log.error("Failed to process auth.user.registered.v1 payload: {}", e.getMessage(), e);
        }
    }
}
