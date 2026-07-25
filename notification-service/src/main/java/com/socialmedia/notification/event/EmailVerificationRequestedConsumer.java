package com.socialmedia.notification.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialmedia.notification.domain.NotificationChannel;
import com.socialmedia.notification.event.incoming.EmailVerificationRequestedEvent;
import com.socialmedia.notification.service.impl.NotificationDispatchService;
import java.util.Map;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class EmailVerificationRequestedConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationDispatchService dispatchService;

    public EmailVerificationRequestedConsumer(ObjectMapper objectMapper, NotificationDispatchService dispatchService) {
        this.objectMapper = objectMapper;
        this.dispatchService = dispatchService;
    }

    @KafkaListener(topics = NotificationTopics.EMAIL_VERIFICATION_REQUESTED, groupId = "${spring.kafka.consumer.group-id}")
    public void onEmailVerificationRequested(String payload) throws Exception {
        EmailVerificationRequestedEvent event = objectMapper.readValue(payload, EmailVerificationRequestedEvent.class);
        dispatchService.dispatch(NotificationChannel.EMAIL, event.userId(), "email-verification",
                Map.of("token", event.verificationToken()), null, event.email(), null);
    }
}
