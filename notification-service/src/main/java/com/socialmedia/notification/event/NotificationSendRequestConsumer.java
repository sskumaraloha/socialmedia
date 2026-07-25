package com.socialmedia.notification.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialmedia.notification.event.outgoing.NotificationSendRequestEvent;
import com.socialmedia.notification.service.impl.NotificationDispatchService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationSendRequestConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationDispatchService dispatchService;

    public NotificationSendRequestConsumer(ObjectMapper objectMapper, NotificationDispatchService dispatchService) {
        this.objectMapper = objectMapper;
        this.dispatchService = dispatchService;
    }

    @KafkaListener(topics = NotificationTopics.NOTIFICATION_SEND_REQUESTED, groupId = "${spring.kafka.consumer.group-id}")
    public void onNotificationSendRequested(String payload) throws Exception {
        NotificationSendRequestEvent event = objectMapper.readValue(payload, NotificationSendRequestEvent.class);
        dispatchService.dispatch(event.channel(), event.userId(), event.templateKey(), event.templateParams(),
                event.locale(), event.recipientEmail(), event.recipientPhone());
    }
}
