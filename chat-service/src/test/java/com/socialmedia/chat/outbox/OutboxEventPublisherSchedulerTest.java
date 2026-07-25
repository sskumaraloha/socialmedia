package com.socialmedia.chat.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@ExtendWith(MockitoExtension.class)
class OutboxEventPublisherSchedulerTest {

    @Mock private OutboxEventRepository outboxEventRepository;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;

    private OutboxEventPublisherScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new OutboxEventPublisherScheduler(outboxEventRepository, kafkaTemplate, new ObjectMapper());
    }

    @SuppressWarnings("unchecked")
    private CompletableFuture<SendResult<String, Object>> successfulSend() {
        return CompletableFuture.completedFuture(mock(SendResult.class));
    }

    @Test
    void marksEveryEventPublishedWhenAllSendsSucceed() {
        OutboxEvent first = new OutboxEvent("chat.created.v1", "key-1", "ChatCreatedEvent", "{}");
        OutboxEvent second = new OutboxEvent("chat.deleted.v1", "key-2", "ChatDeletedEvent", "{}");
        when(outboxEventRepository.findTop100ByPublishedFalseOrderByCreatedAtAsc()).thenReturn(List.of(first, second));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(successfulSend());

        scheduler.publishPending();

        assertThat(first.isPublished()).isTrue();
        assertThat(second.isPublished()).isTrue();
        verify(kafkaTemplate, times(2)).send(anyString(), anyString(), any());
    }

    @Test
    void stopsAtTheFirstFailureInsteadOfSkippingAheadToPreserveOrdering() {
        OutboxEvent first = new OutboxEvent("chat.created.v1", "key-1", "ChatCreatedEvent", "{}");
        OutboxEvent second = new OutboxEvent("chat.deleted.v1", "key-2", "ChatDeletedEvent", "{}");
        when(outboxEventRepository.findTop100ByPublishedFalseOrderByCreatedAtAsc()).thenReturn(List.of(first, second));
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker unreachable")));

        scheduler.publishPending();

        assertThat(first.isPublished()).isFalse();
        assertThat(second.isPublished()).isFalse();
        verify(kafkaTemplate, times(1)).send(anyString(), anyString(), any());
    }
}
