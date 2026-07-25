package com.socialmedia.chat.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateChatRequest(
        @Size(max = 100) String name,
        @Size(max = 500) String description,
        @Size(max = 1000) String avatarUrl
) {
}
