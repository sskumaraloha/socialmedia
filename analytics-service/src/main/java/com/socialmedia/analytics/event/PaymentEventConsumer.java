package com.socialmedia.analytics.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialmedia.analytics.event.incoming.PaymentCompletedEvent;
import com.socialmedia.analytics.service.impl.MetricsWriter;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Forward-declared: no payment-service exists yet in this platform, so this consumer never
 * receives traffic today, but the revenue dashboard is ready the moment one is built. */
@Component
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final MetricsWriter metricsWriter;

    public PaymentEventConsumer(ObjectMapper objectMapper, MetricsWriter metricsWriter) {
        this.objectMapper = objectMapper;
        this.metricsWriter = metricsWriter;
    }

    @KafkaListener(topics = AnalyticsTopics.PAYMENT_COMPLETED, groupId = "${spring.kafka.consumer.group-id}")
    public void onPaymentCompleted(String payload) {
        try {
            PaymentCompletedEvent event = objectMapper.readValue(payload, PaymentCompletedEvent.class);
            LocalDate date = LocalDate.ofInstant(event.occurredAt(), ZoneOffset.UTC);
            metricsWriter.recordRevenue(date, event.amountCents());
        } catch (Exception e) {
            log.error("Failed to process payment.completed.v1 payload: {}", e.getMessage(), e);
        }
    }
}
