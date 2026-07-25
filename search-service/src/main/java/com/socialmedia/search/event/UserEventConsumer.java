package com.socialmedia.search.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialmedia.search.domain.UserDocument;
import com.socialmedia.search.event.incoming.UserProfileCreatedEvent;
import com.socialmedia.search.event.incoming.UserProfileUpdatedEvent;
import com.socialmedia.search.service.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final SearchService searchService;

    public UserEventConsumer(ObjectMapper objectMapper, SearchService searchService) {
        this.objectMapper = objectMapper;
        this.searchService = searchService;
    }

    @KafkaListener(topics = SearchTopics.USER_PROFILE_CREATED, groupId = "${spring.kafka.consumer.group-id}")
    public void onUserProfileCreated(String payload) {
        try {
            UserProfileCreatedEvent event = objectMapper.readValue(payload, UserProfileCreatedEvent.class);
            searchService.indexUser(new UserDocument(event.userId(), event.username(), null, null));
        } catch (Exception e) {
            log.error("Failed to process user.profile.created.v1 payload: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = SearchTopics.USER_PROFILE_UPDATED, groupId = "${spring.kafka.consumer.group-id}")
    public void onUserProfileUpdated(String payload) {
        try {
            UserProfileUpdatedEvent event = objectMapper.readValue(payload, UserProfileUpdatedEvent.class);
            searchService.indexUser(new UserDocument(event.userId(), event.username(), event.displayName(), event.bio()));
        } catch (Exception e) {
            log.error("Failed to process user.profile.updated.v1 payload: {}", e.getMessage(), e);
        }
    }
}
