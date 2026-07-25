package com.socialmedia.user.dto.response;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String username,
        String displayName,
        String bio,
        String avatarUrl,
        boolean verified,
        String customStatus,
        Boolean online,
        Instant lastSeenAt,
        long followersCount,
        long followingCount,
        boolean followedByViewer,
        boolean followingViewer,
        boolean blockedByViewer,
        boolean mutedByViewer
) {
}
