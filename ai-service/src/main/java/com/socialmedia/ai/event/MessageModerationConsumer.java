package com.socialmedia.ai.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialmedia.ai.dto.response.ModerationResponse;
import com.socialmedia.ai.dto.response.SpamCheckResponse;
import com.socialmedia.ai.event.incoming.MessageSentEvent;
import com.socialmedia.ai.event.outgoing.MessageModeratedEvent;
import com.socialmedia.ai.service.AiService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Runs spam/moderation checks on every new message as a best-effort enrichment - a
 * failure here (the AI provider being unreachable) is logged and dropped rather than
 * retried, since blocking or endlessly retrying message delivery on an optional
 * moderation pass would make the AI provider a hard dependency for messaging itself.
 * Only ever sees the truncated, non-encrypted preview message-service already publishes
 * for chat-service's benefit - never message-service's full decrypted content.
 */
@Component
public class MessageModerationConsumer {

    private static final Logger log = LoggerFactory.getLogger(MessageModerationConsumer.class);

    private final ObjectMapper objectMapper;
    private final AiService aiService;
    private final AiEventPublisher eventPublisher;

    public MessageModerationConsumer(ObjectMapper objectMapper, AiService aiService, AiEventPublisher eventPublisher) {
        this.objectMapper = objectMapper;
        this.aiService = aiService;
        this.eventPublisher = eventPublisher;
    }

    @KafkaListener(topics = AiTopics.MESSAGE_SENT, groupId = "${spring.kafka.consumer.group-id}")
    public void onMessageSent(String payload) {
        try {
            MessageSentEvent event = objectMapper.readValue(payload, MessageSentEvent.class);
            if (event.contentPreview() == null || event.contentPreview().isBlank()) {
                return;
            }

            ModerationResponse moderation = aiService.moderate(event.contentPreview());
            SpamCheckResponse spamCheck = aiService.detectSpam(event.contentPreview());

            List<String> categories = new ArrayList<>(moderation.categories());
            String reason = moderation.flagged() ? moderation.reason() : spamCheck.reason();

            eventPublisher.publishMessageModerated(new MessageModeratedEvent(event.messageId(), event.chatId(),
                    event.senderId(), moderation.flagged(), spamCheck.spam(), categories, reason, Instant.now()));
        } catch (Exception e) {
            log.warn("Skipping moderation for a message.sent.v1 payload: {}", e.getMessage());
        }
    }
}
