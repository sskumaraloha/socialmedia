package com.socialmedia.message.service.impl;

import com.socialmedia.message.service.MessageService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polling-based delivery for scheduled and self-destructing messages. A poll interval of
 * a few seconds is an accepted tradeoff for this scope - a production deployment at very
 * large scale would likely use a delay-queue (e.g. a Kafka topic with per-message delay,
 * or a dedicated scheduler service) instead of scanning the whole due-set every tick.
 */
@Component
public class MessageMaintenanceScheduler {

    private final MessageService messageService;

    public MessageMaintenanceScheduler(MessageService messageService) {
        this.messageService = messageService;
    }

    @Scheduled(fixedDelayString = "${app.scheduling.scheduled-message-poll-ms:5000}")
    public void publishDueScheduledMessages() {
        messageService.publishDueScheduledMessages();
    }

    @Scheduled(fixedDelayString = "${app.scheduling.self-destruct-poll-ms:5000}")
    public void purgeDueSelfDestructMessages() {
        messageService.purgeDueSelfDestructMessages();
    }
}
