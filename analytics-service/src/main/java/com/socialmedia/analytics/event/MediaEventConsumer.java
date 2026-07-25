package com.socialmedia.analytics.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialmedia.analytics.event.incoming.MediaUploadedEvent;
import com.socialmedia.analytics.service.impl.MetricsWriter;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class MediaEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(MediaEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final MetricsWriter metricsWriter;

    public MediaEventConsumer(ObjectMapper objectMapper, MetricsWriter metricsWriter) {
        this.objectMapper = objectMapper;
        this.metricsWriter = metricsWriter;
    }

    @KafkaListener(topics = AnalyticsTopics.MEDIA_UPLOADED, groupId = "${spring.kafka.consumer.group-id}")
    public void onMediaUploaded(String payload) {
        try {
            MediaUploadedEvent event = objectMapper.readValue(payload, MediaUploadedEvent.class);
            LocalDate date = LocalDate.ofInstant(event.occurredAt(), ZoneOffset.UTC);
            metricsWriter.recordStorage(date, event.sizeBytes());
        } catch (Exception e) {
            log.error("Failed to process media.uploaded.v1 payload: {}", e.getMessage(), e);
        }
    }
}
