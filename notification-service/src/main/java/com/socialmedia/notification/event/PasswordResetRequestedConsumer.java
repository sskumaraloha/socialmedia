package com.socialmedia.notification.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialmedia.notification.domain.NotificationChannel;
import com.socialmedia.notification.event.incoming.PasswordResetRequestedEvent;
import com.socialmedia.notification.service.impl.NotificationDispatchService;
import java.util.Map;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PasswordResetRequestedConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationDispatchService dispatchService;

    public PasswordResetRequestedConsumer(ObjectMapper objectMapper, NotificationDispatchService dispatchService) {
        this.objectMapper = objectMapper;
        this.dispatchService = dispatchService;
    }

    @KafkaListener(topics = NotificationTopics.PASSWORD_RESET_REQUESTED, groupId = "${spring.kafka.consumer.group-id}")
    public void onPasswordResetRequested(String payload) throws Exception {
        PasswordResetRequestedEvent event = objectMapper.readValue(payload, PasswordResetRequestedEvent.class);
        dispatchService.dispatch(NotificationChannel.EMAIL, event.userId(), "password-reset",
                Map.of("token", event.resetToken()), null, event.email(), null);
    }
}
