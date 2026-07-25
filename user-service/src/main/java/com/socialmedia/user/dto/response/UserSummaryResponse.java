package com.socialmedia.user.dto.response;

import java.util.UUID;

/** Lightweight shape for search results, suggestions, and follower/following list items. */
public record UserSummaryResponse(
        UUID id,
        String username,
        String displayName,
        String avatarUrl,
        boolean verified
) {
}
