package com.socialmedia.chat.dto.response;

import com.socialmedia.chat.domain.ChatMemberRole;
import com.socialmedia.chat.domain.ChatType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ChatDetailResponse(
        UUID id,
        ChatType type,
        String name,
        String description,
        String avatarUrl,
        UUID createdBy,
        boolean broadcastOnly,
        Instant createdAt,
        List<ChatMemberResponse> members,
        ChatMemberRole myRole,
        boolean muted,
        boolean archived,
        int unreadCount
) {
}
