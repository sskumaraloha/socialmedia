package com.socialmedia.analytics.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialmedia.analytics.domain.AuditLogEntry;
import com.socialmedia.analytics.repository.AuditLogEntryRepository;
import com.socialmedia.common.audit.AuditEvent;
import com.socialmedia.common.audit.AuditEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Unlike every other consumer in this platform, this deserializes common-library's own
 * AuditEvent directly rather than a local per-service mirror - AuditEvent is deliberately a
 * shared wire-contract type (every producer across the platform already imports the exact
 * same class from common-library to construct it via AuditEventPublisher), not a domain
 * object owned by any one service, so mirroring it here would just be copying a type that's
 * already meant to be shared.
 */
@Component
public class AuditEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final AuditLogEntryRepository auditLogEntryRepository;

    public AuditEventConsumer(ObjectMapper objectMapper, AuditLogEntryRepository auditLogEntryRepository) {
        this.objectMapper = objectMapper;
        this.auditLogEntryRepository = auditLogEntryRepository;
    }

    @KafkaListener(topics = AuditEventPublisher.TOPIC, groupId = "${spring.kafka.consumer.group-id}")
    public void onAuditEvent(String payload) {
        try {
            AuditEvent event = objectMapper.readValue(payload, AuditEvent.class);
            auditLogEntryRepository.save(new AuditLogEntry(event.actorUserId(), event.action(), event.targetType(),
                    event.targetId(), event.metadata(), event.occurredAt()));
        } catch (Exception e) {
            log.error("Failed to process audit.event.v1 payload: {}", e.getMessage(), e);
        }
    }
}
