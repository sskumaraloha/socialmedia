package com.socialmedia.chat.dto.response;

import com.socialmedia.chat.domain.ChatMemberRole;
import com.socialmedia.chat.domain.ChatType;
import java.time.Instant;
import java.util.UUID;

public record ChatSummaryResponse(
        UUID id,
        ChatType type,
        String name,
        String avatarUrl,
        Instant lastMessageAt,
        String lastMessagePreview,
        int unreadCount,
        boolean muted,
        boolean archived,
        ChatMemberRole myRole
) {
}
