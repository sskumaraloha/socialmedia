package com.socialmedia.message.service;

import com.socialmedia.message.domain.EncryptedEnvelope;
import com.socialmedia.message.dto.request.DraftRequest;
import com.socialmedia.message.dto.request.EditMessageRequest;
import com.socialmedia.message.dto.request.SendMessageRequest;
import com.socialmedia.message.dto.response.DraftResponse;
import com.socialmedia.message.dto.response.MessageResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Every read path now takes the caller's {@code deviceId} (from the JWT's own deviceId claim)
 * as well as their userId: with per-device end-to-end encryption, "which ciphertext do you get"
 * is a per-device question, not a per-user one.
 */
public interface MessageService {

    MessageResponse sendMessage(UUID chatId, UUID senderId, String senderDeviceId, String bearerToken,
            SendMessageRequest request);

    MessageResponse getMessage(UUID messageId, UUID viewerId, String viewerDeviceId, String bearerToken);

    List<MessageResponse> listMessages(UUID chatId, UUID viewerId, String viewerDeviceId, String bearerToken,
            Instant before, int limit);

    MessageResponse editMessage(UUID messageId, UUID requesterId, String requesterDeviceId, EditMessageRequest request);

    void deleteMessage(UUID messageId, UUID requesterId, boolean forEveryone);

    MessageResponse forwardMessage(UUID messageId, UUID requesterId, String requesterDeviceId, String bearerToken,
            UUID targetChatId, List<EncryptedEnvelope> reEncryptedEnvelopes);

    MessageResponse reactToMessage(UUID messageId, UUID userId, String userDeviceId, String emoji);

    void removeReaction(UUID messageId, UUID userId);

    void markDelivered(UUID messageId, UUID userId);

    void markRead(UUID messageId, UUID userId);

    MessageResponse starMessage(UUID messageId, UUID userId, String userDeviceId);

    void unstarMessage(UUID messageId, UUID userId);

    List<MessageResponse> listStarredMessages(UUID userId, String userDeviceId);

    DraftResponse saveDraft(UUID chatId, UUID userId, DraftRequest request);

    DraftResponse getDraft(UUID chatId, UUID userId);

    void deleteDraft(UUID chatId, UUID userId);

    int publishDueScheduledMessages();

    int purgeDueSelfDestructMessages();
}
