package com.socialmedia.search.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialmedia.search.domain.MediaDocument;
import com.socialmedia.search.event.incoming.MediaDeletedEvent;
import com.socialmedia.search.event.incoming.MediaQuarantinedEvent;
import com.socialmedia.search.event.incoming.MediaUploadedEvent;
import com.socialmedia.search.service.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class MediaEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(MediaEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final SearchService searchService;

    public MediaEventConsumer(ObjectMapper objectMapper, SearchService searchService) {
        this.objectMapper = objectMapper;
        this.searchService = searchService;
    }

    @KafkaListener(topics = SearchTopics.MEDIA_UPLOADED, groupId = "${spring.kafka.consumer.group-id}")
    public void onMediaUploaded(String payload) {
        try {
            MediaUploadedEvent event = objectMapper.readValue(payload, MediaUploadedEvent.class);
            searchService.indexMedia(new MediaDocument(event.mediaId(), event.ownerId(), event.originalFilename(), event.kind()));
        } catch (Exception e) {
            log.error("Failed to process media.uploaded.v1 payload: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = SearchTopics.MEDIA_DELETED, groupId = "${spring.kafka.consumer.group-id}")
    public void onMediaDeleted(String payload) {
        try {
            MediaDeletedEvent event = objectMapper.readValue(payload, MediaDeletedEvent.class);
            searchService.deleteMedia(event.mediaId());
        } catch (Exception e) {
            log.error("Failed to process media.deleted.v1 payload: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = SearchTopics.MEDIA_QUARANTINED, groupId = "${spring.kafka.consumer.group-id}")
    public void onMediaQuarantined(String payload) {
        try {
            MediaQuarantinedEvent event = objectMapper.readValue(payload, MediaQuarantinedEvent.class);
            searchService.deleteMedia(event.mediaId());
        } catch (Exception e) {
            log.error("Failed to process media.quarantined.v1 payload: {}", e.getMessage(), e);
        }
    }
}
