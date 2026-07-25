package com.socialmedia.analytics.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialmedia.analytics.event.incoming.MessageSentEvent;
import com.socialmedia.analytics.service.impl.MetricsWriter;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class MessageEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(MessageEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final MetricsWriter metricsWriter;

    public MessageEventConsumer(ObjectMapper objectMapper, MetricsWriter metricsWriter) {
        this.objectMapper = objectMapper;
        this.metricsWriter = metricsWriter;
    }

    @KafkaListener(topics = AnalyticsTopics.MESSAGE_SENT, groupId = "${spring.kafka.consumer.group-id}")
    public void onMessageSent(String payload) {
        try {
            MessageSentEvent event = objectMapper.readValue(payload, MessageSentEvent.class);
            metricsWriter.recordMessage(LocalDate.ofInstant(event.sentAt(), ZoneOffset.UTC));
        } catch (Exception e) {
            log.error("Failed to process message.sent.v1 payload: {}", e.getMessage(), e);
        }
    }
}
