package com.socialmedia.chat.dto.response;

import com.socialmedia.chat.domain.ChatMemberRole;
import com.socialmedia.chat.domain.ChatType;
import java.time.Instant;
import java.util.UUID;

/** No lastMessagePreview: with end-to-end encrypted content the server cannot produce one, so
 * clients render the preview from their own locally-decrypted copy of the last message. */
public record ChatSummaryResponse(
        UUID id,
        ChatType type,
        String name,
        String avatarUrl,
        Instant lastMessageAt,
        int unreadCount,
        boolean muted,
        boolean archived,
        ChatMemberRole myRole
) {
}
