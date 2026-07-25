package com.socialmedia.message.client;

import java.util.List;
import java.util.UUID;

/**
 * Minimal local view of chat-service's ChatDetailResponse - only the fields message-service
 * actually needs. Unknown JSON properties are ignored (Spring Boot's default Jackson
 * config), so this can drift from the full response shape without breaking.
 */
public record ChatMembersView(List<ChatMemberView> members) {

    public record ChatMemberView(UUID userId) {
    }
}
