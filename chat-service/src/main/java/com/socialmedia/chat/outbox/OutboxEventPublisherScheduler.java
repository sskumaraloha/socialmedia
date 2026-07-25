package com.socialmedia.chat.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The other half of the transactional outbox: polls for events staged by OutboxEventPublisher
 * and actually delivers them to Kafka, marking each published only after a successful send.
 * This is the standard "polling publisher" implementation of the pattern - simpler than
 * change-data-capture (e.g. Debezium tailing the WAL) at the cost of poll-interval latency,
 * an acceptable tradeoff at this scale. At-least-once delivery: if the process crashes
 * between a successful send and marking the row published, the event is resent on the next
 * poll - every consumer of these topics already tolerates duplicates by design (Kafka
 * delivery in this platform has never promised exactly-once).
 *
 * <p>Reuses the service's single default KafkaTemplate&lt;String, Object&gt; (the same bean
 * every other producer in the platform uses, configured with Spring Kafka's JsonSerializer)
 * rather than standing up a second Kafka producer bean. The outbox row's payload is already a
 * JSON string (see ChatEventPublisher.stage); parsing it back into a JsonNode before sending
 * lets JsonSerializer write that tree out as-is - sending the raw String directly would
 * instead serialize it AS a JSON string literal (quoted and escaped), corrupting the payload
 * every consumer expects to Jackson-parse into its own event type.
 */
@Component
public class OutboxEventPublisherScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventPublisherScheduler.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OutboxEventPublisherScheduler(OutboxEventRepository outboxEventRepository,
            KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:500}")
    @Transactional
    public void publishPending() {
        List<OutboxEvent> batch = outboxEventRepository.findTop100ByPublishedFalseOrderByCreatedAtAsc();
        for (OutboxEvent event : batch) {
            try {
                JsonNode payload = objectMapper.readTree(event.getPayload());
                kafkaTemplate.send(event.getTopic(), event.getAggregateKey(), payload).get(5, TimeUnit.SECONDS);
                event.markPublished();
            } catch (Exception e) {
                log.error("Failed to publish outbox event {} (topic={}, key={}): {} - will retry next poll",
                        event.getId(), event.getTopic(), event.getAggregateKey(), e.getMessage());
                // Stop here rather than skip ahead to the next row: preserves per-topic/key
                // ordering by not racing a later event for the same aggregate past this one.
                break;
            }
        }
    }
}
