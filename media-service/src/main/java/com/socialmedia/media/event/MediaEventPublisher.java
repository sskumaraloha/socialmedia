package com.socialmedia.media.event;

import com.socialmedia.media.domain.MediaAsset;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class MediaEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(MediaEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public MediaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishUploaded(MediaAsset asset) {
        send(MediaTopics.MEDIA_UPLOADED, asset.getId().toString(),
                new MediaUploadedEvent(asset.getId(), asset.getOwnerId(), asset.getOriginalFilename(), asset.getKind(), Instant.now()));
    }

    public void publishProcessed(MediaAsset asset) {
        send(MediaTopics.MEDIA_PROCESSED, asset.getId().toString(),
                new MediaProcessedEvent(asset.getId(), asset.getOwnerId(), asset.getThumbnailKey() != null,
                        asset.isTranscoded(), Instant.now()));
    }

    public void publishQuarantined(MediaAsset asset) {
        send(MediaTopics.MEDIA_QUARANTINED, asset.getId().toString(),
                new MediaQuarantinedEvent(asset.getId(), asset.getOwnerId(), Instant.now()));
    }

    public void publishDeleted(UUID mediaId, UUID ownerId) {
        send(MediaTopics.MEDIA_DELETED, mediaId.toString(), new MediaDeletedEvent(mediaId, ownerId, Instant.now()));
    }

    private void send(String topic, String key, Object payload) {
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
