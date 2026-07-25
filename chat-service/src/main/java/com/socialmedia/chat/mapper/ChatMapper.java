package com.socialmedia.chat.mapper;

import com.socialmedia.chat.domain.Chat;
import com.socialmedia.chat.domain.ChatMember;
import com.socialmedia.chat.domain.PinnedMessage;
import com.socialmedia.chat.dto.response.ChatDetailResponse;
import com.socialmedia.chat.dto.response.ChatMemberResponse;
import com.socialmedia.chat.dto.response.ChatSummaryResponse;
import com.socialmedia.chat.dto.response.PinnedMessageResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ChatMapper {

    public ChatSummaryResponse toSummary(Chat chat, ChatMember viewerMembership) {
        return new ChatSummaryResponse(
                chat.getId(),
                chat.getType(),
                chat.getName(),
                chat.getAvatarUrl(),
                chat.getLastMessageAt(),
                chat.getLastMessagePreview(),
                viewerMembership.getUnreadCount(),
                viewerMembership.isMuted(),
                viewerMembership.isArchived(),
                viewerMembership.getRole());
    }

    public ChatDetailResponse toDetail(Chat chat, ChatMember viewerMembership, List<ChatMember> allMembers) {
        List<ChatMemberResponse> memberResponses = allMembers.stream()
                .map(m -> new ChatMemberResponse(m.getUserId(), m.getRole(), m.getCreatedAt()))
                .toList();
        return new ChatDetailResponse(
                chat.getId(),
                chat.getType(),
                chat.getName(),
                chat.getDescription(),
                chat.getAvatarUrl(),
                chat.getCreatedBy(),
                chat.isBroadcastOnly(),
                chat.getCreatedAt(),
                memberResponses,
                viewerMembership.getRole(),
                viewerMembership.isMuted(),
                viewerMembership.isArchived(),
                viewerMembership.getUnreadCount());
    }

    public PinnedMessageResponse toPinnedResponse(PinnedMessage pinnedMessage) {
        return new PinnedMessageResponse(pinnedMessage.getMessageId(), pinnedMessage.getPinnedBy(), pinnedMessage.getCreatedAt());
    }
}
