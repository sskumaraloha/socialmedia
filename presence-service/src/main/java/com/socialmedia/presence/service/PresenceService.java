package com.socialmedia.presence.service;

import com.socialmedia.presence.dto.response.PresenceResponse;
import java.util.List;
import java.util.UUID;

public interface PresenceService {

    void heartbeat(UUID userId);

    void goOffline(UUID userId);

    PresenceResponse getPresence(UUID userId);

    List<PresenceResponse> getPresence(List<UUID> userIds);

    int sweepStaleUsers();
}
