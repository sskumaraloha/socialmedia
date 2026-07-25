package com.socialmedia.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

public record CreateGroupChatRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description,
        @NotEmpty Set<UUID> memberIds,
        boolean channel,
        boolean broadcastOnly
) {
}
