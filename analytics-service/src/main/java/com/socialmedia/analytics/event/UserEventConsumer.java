package com.socialmedia.analytics.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialmedia.analytics.event.incoming.UserLoggedInEvent;
import com.socialmedia.analytics.event.incoming.UserRegisteredEvent;
import com.socialmedia.analytics.service.impl.MetricsWriter;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final MetricsWriter metricsWriter;

    public UserEventConsumer(ObjectMapper objectMapper, MetricsWriter metricsWriter) {
        this.objectMapper = objectMapper;
        this.metricsWriter = metricsWriter;
    }

    @KafkaListener(topics = AnalyticsTopics.USER_REGISTERED, groupId = "${spring.kafka.consumer.group-id}")
    public void onUserRegistered(String payload) {
        try {
            UserRegisteredEvent event = objectMapper.readValue(payload, UserRegisteredEvent.class);
            LocalDate date = LocalDate.ofInstant(event.occurredAt(), ZoneOffset.UTC);
            metricsWriter.recordSignup(date);
            metricsWriter.recordSignupActivity(event.userId(), date);
        } catch (Exception e) {
            log.error("Failed to process auth.user.registered.v1 payload: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = AnalyticsTopics.USER_LOGGED_IN, groupId = "${spring.kafka.consumer.group-id}")
    public void onUserLoggedIn(String payload) {
        try {
            UserLoggedInEvent event = objectMapper.readValue(payload, UserLoggedInEvent.class);
            LocalDate date = LocalDate.ofInstant(event.occurredAt(), ZoneOffset.UTC);
            metricsWriter.recordActiveUser(date, event.userId());
            metricsWriter.recordLoginActivity(event.userId(), date);
        } catch (Exception e) {
            log.error("Failed to process auth.user.logged-in.v1 payload: {}", e.getMessage(), e);
        }
    }
}
