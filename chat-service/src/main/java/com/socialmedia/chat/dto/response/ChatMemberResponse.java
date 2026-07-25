package com.socialmedia.chat.dto.response;

import com.socialmedia.chat.domain.ChatMemberRole;
import java.time.Instant;
import java.util.UUID;

public record ChatMemberResponse(UUID userId, ChatMemberRole role, Instant joinedAt) {
}
