package com.socialmedia.message.service;

import com.socialmedia.message.dto.request.DraftRequest;
import com.socialmedia.message.dto.request.EditMessageRequest;
import com.socialmedia.message.dto.request.SendMessageRequest;
import com.socialmedia.message.dto.response.DraftResponse;
import com.socialmedia.message.dto.response.MessageResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MessageService {

    MessageResponse sendMessage(UUID chatId, UUID senderId, String bearerToken, SendMessageRequest request);

    MessageResponse getMessage(UUID messageId, UUID viewerId, String bearerToken);

    List<MessageResponse> listMessages(UUID chatId, UUID viewerId, String bearerToken, Instant before, int limit);

    MessageResponse editMessage(UUID messageId, UUID requesterId, EditMessageRequest request);

    void deleteMessage(UUID messageId, UUID requesterId, boolean forEveryone);

    MessageResponse forwardMessage(UUID messageId, UUID requesterId, String bearerToken, UUID targetChatId);

    MessageResponse reactToMessage(UUID messageId, UUID userId, String emoji);

    void removeReaction(UUID messageId, UUID userId);

    void markDelivered(UUID messageId, UUID userId);

    void markRead(UUID messageId, UUID userId);

    MessageResponse starMessage(UUID messageId, UUID userId);

    void unstarMessage(UUID messageId, UUID userId);

    List<MessageResponse> listStarredMessages(UUID userId);

    DraftResponse saveDraft(UUID chatId, UUID userId, DraftRequest request);

    DraftResponse getDraft(UUID chatId, UUID userId);

    void deleteDraft(UUID chatId, UUID userId);

    int publishDueScheduledMessages();

    int purgeDueSelfDestructMessages();
}
