package com.socialmedia.user.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 100) String displayName,
        @Size(max = 500) String bio,
        String avatarUrl,
        @Size(max = 100) String customStatus
) {
}
