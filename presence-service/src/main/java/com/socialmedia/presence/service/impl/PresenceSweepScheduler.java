package com.socialmedia.presence.service.impl;

import com.socialmedia.presence.service.PresenceService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PresenceSweepScheduler {

    private final PresenceService presenceService;

    public PresenceSweepScheduler(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @Scheduled(fixedDelayString = "${app.presence.sweep-interval-ms:15000}")
    public void sweepStaleUsers() {
        presenceService.sweepStaleUsers();
    }
}
