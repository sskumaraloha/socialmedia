package com.socialmedia.presence.controller;

import com.socialmedia.presence.dto.response.PresenceResponse;
import com.socialmedia.presence.service.PresenceService;
import com.socialmedia.common.security.CurrentUser;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/presence")
public class PresenceController {

    private final PresenceService presenceService;

    public PresenceController(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @PostMapping("/heartbeat")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void heartbeat() {
        presenceService.heartbeat(CurrentUser.get().userId());
    }

    @DeleteMapping("/heartbeat")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void goOffline() {
        presenceService.goOffline(CurrentUser.get().userId());
    }

    @GetMapping("/me")
    public PresenceResponse getMyPresence() {
        return presenceService.getPresence(CurrentUser.get().userId());
    }

    @GetMapping("/{userId}")
    public PresenceResponse getPresence(@PathVariable UUID userId) {
        return presenceService.getPresence(userId);
    }

    @GetMapping
    public List<PresenceResponse> getPresenceBulk(@RequestParam List<UUID> userIds) {
        return presenceService.getPresence(userIds);
    }
}
