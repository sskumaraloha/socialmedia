package com.socialmedia.chat.service;

import com.socialmedia.chat.dto.request.CreateGroupChatRequest;
import com.socialmedia.chat.dto.request.UpdateChatRequest;
import com.socialmedia.chat.dto.response.ChatDetailResponse;
import com.socialmedia.chat.dto.response.ChatSummaryResponse;
import com.socialmedia.chat.dto.response.PinnedMessageResponse;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface ChatService {

    ChatDetailResponse createPrivateChat(UUID requesterId, UUID recipientId);

    ChatDetailResponse createGroupChat(UUID requesterId, CreateGroupChatRequest request, String bearerAuthorizationHeader);

    ChatDetailResponse getChat(UUID chatId, UUID viewerId);

    List<ChatSummaryResponse> listMyChats(UUID viewerId, boolean includeArchived);

    List<ChatSummaryResponse> searchMyChats(UUID viewerId, String query);

    ChatDetailResponse updateChat(UUID chatId, UUID requesterId, UpdateChatRequest request);

    void deleteChat(UUID chatId, UUID requesterId);

    void addMembers(UUID chatId, UUID requesterId, Set<UUID> userIds);

    void removeMember(UUID chatId, UUID requesterId, UUID targetUserId);

    void changeRole(UUID chatId, UUID requesterId, UUID targetUserId, com.socialmedia.chat.domain.ChatMemberRole newRole);

    void muteChat(UUID chatId, UUID requesterId, Instant mutedUntil);

    void unmuteChat(UUID chatId, UUID requesterId);

    void archiveChat(UUID chatId, UUID requesterId);

    void unarchiveChat(UUID chatId, UUID requesterId);

    void markRead(UUID chatId, UUID requesterId);

    PinnedMessageResponse pinMessage(UUID chatId, UUID requesterId, UUID messageId);

    void unpinMessage(UUID chatId, UUID requesterId, UUID messageId);

    List<PinnedMessageResponse> listPinnedMessages(UUID chatId, UUID viewerId);

    void recordIncomingMessage(UUID chatId, UUID senderId, String contentPreview, Instant sentAt);
}
