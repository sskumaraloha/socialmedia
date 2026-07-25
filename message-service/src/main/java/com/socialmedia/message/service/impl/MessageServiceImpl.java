package com.socialmedia.message.service.impl;

import com.socialmedia.message.client.ChatMembershipClient;
import com.socialmedia.message.domain.ChatMembership;
import com.socialmedia.message.domain.Draft;
import com.socialmedia.message.domain.MediaMetadata;
import com.socialmedia.message.domain.Message;
import com.socialmedia.message.domain.StarredMessage;
import com.socialmedia.message.dto.request.DraftRequest;
import com.socialmedia.message.dto.request.EditMessageRequest;
import com.socialmedia.message.dto.request.MediaRequest;
import com.socialmedia.message.dto.request.SendMessageRequest;
import com.socialmedia.message.dto.response.DraftResponse;
import com.socialmedia.message.dto.response.MessageResponse;
import com.socialmedia.message.event.MessageEventPublisher;
import com.socialmedia.message.exception.NotAChatMemberException;
import com.socialmedia.message.exception.NotMessageAuthorException;
import com.socialmedia.message.mapper.MessageMapper;
import com.socialmedia.message.repository.ChatMembershipRepository;
import com.socialmedia.message.repository.DraftRepository;
import com.socialmedia.message.repository.MessageRepository;
import com.socialmedia.message.repository.StarredMessageRepository;
import com.socialmedia.message.service.MessageEncryptionService;
import com.socialmedia.message.service.MessageService;
import com.socialmedia.message.websocket.MessageWebSocketNotifier;
import com.socialmedia.common.exception.BusinessException;
import com.socialmedia.common.exception.ConflictException;
import com.socialmedia.common.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class MessageServiceImpl implements MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageServiceImpl.class);

    private final MessageRepository messageRepository;
    private final DraftRepository draftRepository;
    private final StarredMessageRepository starredMessageRepository;
    private final ChatMembershipRepository chatMembershipRepository;
    private final ChatMembershipClient chatMembershipClient;
    private final MessageEncryptionService encryptionService;
    private final MessageEventPublisher eventPublisher;
    private final MessageWebSocketNotifier webSocketNotifier;
    private final MessageMapper mapper;

    public MessageServiceImpl(MessageRepository messageRepository, DraftRepository draftRepository,
            StarredMessageRepository starredMessageRepository, ChatMembershipRepository chatMembershipRepository,
            ChatMembershipClient chatMembershipClient, MessageEncryptionService encryptionService,
            MessageEventPublisher eventPublisher, MessageWebSocketNotifier webSocketNotifier, MessageMapper mapper) {
        this.messageRepository = messageRepository;
        this.draftRepository = draftRepository;
        this.starredMessageRepository = starredMessageRepository;
        this.chatMembershipRepository = chatMembershipRepository;
        this.chatMembershipClient = chatMembershipClient;
        this.encryptionService = encryptionService;
        this.eventPublisher = eventPublisher;
        this.webSocketNotifier = webSocketNotifier;
        this.mapper = mapper;
    }

    @Override
    public MessageResponse sendMessage(UUID chatId, UUID senderId, String bearerToken, SendMessageRequest request) {
        Set<UUID> memberIds = chatMembershipClient.fetchMemberIds(chatId, bearerToken);

        if (request.replyToMessageId() != null) {
            Message replyTarget = messageRepository.findById(request.replyToMessageId())
                    .orElseThrow(() -> new ResourceNotFoundException("Message", request.replyToMessageId()));
            if (!replyTarget.getChatId().equals(chatId)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "REPLY_TARGET_WRONG_CHAT",
                        "The message being replied to belongs to a different chat");
            }
        }

        MediaMetadata media = toMediaMetadata(request.media());
        Instant selfDestructAt = request.selfDestructSeconds() == null ? null
                : Instant.now().plusSeconds(request.selfDestructSeconds());

        Message message = new Message(chatId, senderId, request.type(), encryptionService.encrypt(request.content()),
                media, request.replyToMessageId(), null, request.scheduledAt(), selfDestructAt);
        messageRepository.save(message);

        if (message.isSent()) {
            eventPublisher.publishMessageSent(message, request.content());
            notifyNewMessage(message, request.content(), memberIds, senderId);
        }

        return mapper.toResponse(message, request.content());
    }

    @Override
    public MessageResponse getMessage(UUID messageId, UUID viewerId, String bearerToken) {
        Message message = requireVisibleMessage(messageId, viewerId);
        chatMembershipClient.fetchMemberIds(message.getChatId(), bearerToken);
        return mapper.toResponse(message, encryptionService.decrypt(message.getContent()));
    }

    @Override
    public List<MessageResponse> listMessages(UUID chatId, UUID viewerId, String bearerToken, Instant before, int limit) {
        chatMembershipClient.fetchMemberIds(chatId, bearerToken);
        Pageable page = PageRequest.of(0, Math.max(1, Math.min(limit, 200)));
        List<Message> messages = before == null
                ? messageRepository.findAllByChatIdOrderByCreatedAtDesc(chatId, page)
                : messageRepository.findAllByChatIdAndCreatedAtBeforeOrderByCreatedAtDesc(chatId, before, page);

        return messages.stream()
                .filter(m -> m.isSent() && m.isVisibleTo(viewerId))
                .map(m -> mapper.toResponse(m, encryptionService.decrypt(m.getContent())))
                .toList();
    }

    @Override
    public MessageResponse editMessage(UUID messageId, UUID requesterId, EditMessageRequest request) {
        Message message = requireVisibleMessage(messageId, requesterId);
        requireAuthor(message, requesterId, "edit");
        if (message.isDeletedForEveryone()) {
            throw new ConflictException("Cannot edit a deleted message");
        }

        message.edit(encryptionService.encrypt(request.content()));
        messageRepository.save(message);

        notifyOthers(message, requesterId, "MESSAGE_EDITED", message.getId());
        return mapper.toResponse(message, request.content());
    }

    @Override
    public void deleteMessage(UUID messageId, UUID requesterId, boolean forEveryone) {
        Message message = requireVisibleMessage(messageId, requesterId);

        if (forEveryone) {
            requireAuthor(message, requesterId, "delete");
            message.deleteForEveryone();
            eventPublisher.publishMessageDeleted(message.getId(), message.getChatId(), requesterId);
            notifyOthers(message, requesterId, "MESSAGE_DELETED", message.getId());
        } else {
            message.deleteForUser(requesterId);
        }
        messageRepository.save(message);
    }

    @Override
    public MessageResponse forwardMessage(UUID messageId, UUID requesterId, String bearerToken, UUID targetChatId) {
        Message original = requireVisibleMessage(messageId, requesterId);
        Set<UUID> targetMemberIds = chatMembershipClient.fetchMemberIds(targetChatId, bearerToken);

        Message forwarded = new Message(targetChatId, requesterId, original.getType(), original.getContent(),
                original.getMedia(), null, original.getForwardedFromMessageId() != null
                        ? original.getForwardedFromMessageId() : original.getId(),
                null, null);
        messageRepository.save(forwarded);

        String decrypted = encryptionService.decrypt(original.getContent());
        eventPublisher.publishMessageSent(forwarded, decrypted);
        notifyNewMessage(forwarded, decrypted, targetMemberIds, requesterId);

        return mapper.toResponse(forwarded, decrypted);
    }

    @Override
    public MessageResponse reactToMessage(UUID messageId, UUID userId, String emoji) {
        Message message = requireVisibleMessage(messageId, userId);
        requireLocalMembership(message.getChatId(), userId);

        message.upsertReaction(userId, emoji);
        messageRepository.save(message);

        notifyOthers(message, userId, "REACTION_ADDED", messageId);
        return mapper.toResponse(message, encryptionService.decrypt(message.getContent()));
    }

    @Override
    public void removeReaction(UUID messageId, UUID userId) {
        Message message = requireVisibleMessage(messageId, userId);
        message.removeReaction(userId);
        messageRepository.save(message);
        notifyOthers(message, userId, "REACTION_REMOVED", messageId);
    }

    @Override
    public void markDelivered(UUID messageId, UUID userId) {
        Message message = requireVisibleMessage(messageId, userId);
        requireLocalMembership(message.getChatId(), userId);
        message.receiptFor(userId).markDelivered(Instant.now());
        messageRepository.save(message);
    }

    @Override
    public void markRead(UUID messageId, UUID userId) {
        Message message = requireVisibleMessage(messageId, userId);
        requireLocalMembership(message.getChatId(), userId);
        message.receiptFor(userId).markRead(Instant.now());
        messageRepository.save(message);
        notifyOthers(message, userId, "MESSAGE_READ", messageId);
    }

    @Override
    public MessageResponse starMessage(UUID messageId, UUID userId) {
        Message message = requireVisibleMessage(messageId, userId);
        starredMessageRepository.findByUserIdAndMessageId(userId, messageId)
                .orElseGet(() -> starredMessageRepository.save(new StarredMessage(userId, messageId)));
        return mapper.toResponse(message, encryptionService.decrypt(message.getContent()));
    }

    @Override
    public void unstarMessage(UUID messageId, UUID userId) {
        starredMessageRepository.deleteByUserIdAndMessageId(userId, messageId);
    }

    @Override
    public List<MessageResponse> listStarredMessages(UUID userId) {
        List<UUID> messageIds = starredMessageRepository.findAllByUserIdOrderByStarredAtDesc(userId).stream()
                .map(StarredMessage::getMessageId)
                .toList();
        return messageRepository.findAllByIdIn(messageIds).stream()
                .filter(m -> m.isVisibleTo(userId))
                .map(m -> mapper.toResponse(m, encryptionService.decrypt(m.getContent())))
                .toList();
    }

    @Override
    public DraftResponse saveDraft(UUID chatId, UUID userId, DraftRequest request) {
        Draft draft = draftRepository.findByChatIdAndUserId(chatId, userId).orElse(null);
        if (draft == null) {
            draft = new Draft(chatId, userId, request.content());
        } else {
            draft.setContent(request.content());
        }
        draftRepository.save(draft);
        return mapper.toDraftResponse(draft);
    }

    @Override
    public DraftResponse getDraft(UUID chatId, UUID userId) {
        return draftRepository.findByChatIdAndUserId(chatId, userId)
                .map(mapper::toDraftResponse)
                .orElse(new DraftResponse(chatId, "", null));
    }

    @Override
    public void deleteDraft(UUID chatId, UUID userId) {
        draftRepository.deleteByChatIdAndUserId(chatId, userId);
    }

    @Override
    public int publishDueScheduledMessages() {
        List<Message> due = messageRepository.findAllBySentFalseAndScheduledAtLessThanEqual(Instant.now());
        for (Message message : due) {
            message.markSent();
            messageRepository.save(message);
            String decrypted = encryptionService.decrypt(message.getContent());
            eventPublisher.publishMessageSent(message, decrypted);
            List<UUID> members = chatMembershipRepository.findAllByChatId(message.getChatId()).stream()
                    .map(ChatMembership::getUserId).toList();
            notifyNewMessage(message, decrypted, Set.copyOf(members), message.getSenderId());
        }
        if (!due.isEmpty()) {
            log.info("Published {} due scheduled message(s)", due.size());
        }
        return due.size();
    }

    @Override
    public int purgeDueSelfDestructMessages() {
        List<Message> due = messageRepository.findAllBySelfDestructedFalseAndSelfDestructAtLessThanEqual(Instant.now());
        for (Message message : due) {
            message.selfDestruct();
            messageRepository.save(message);
            notifyOthers(message, null, "MESSAGE_SELF_DESTRUCTED", message.getId());
        }
        if (!due.isEmpty()) {
            log.info("Purged {} self-destructed message(s)", due.size());
        }
        return due.size();
    }

    private Message requireVisibleMessage(UUID messageId, UUID viewerId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message", messageId));
        if (!message.isVisibleTo(viewerId)) {
            throw new ResourceNotFoundException("Message", messageId);
        }
        return message;
    }

    private void requireAuthor(Message message, UUID requesterId, String action) {
        if (!message.getSenderId().equals(requesterId)) {
            throw new NotMessageAuthorException(action);
        }
    }

    private void requireLocalMembership(UUID chatId, UUID userId) {
        if (!chatMembershipRepository.existsByChatIdAndUserId(chatId, userId)) {
            throw new NotAChatMemberException();
        }
    }

    private void notifyNewMessage(Message message, String decryptedContent, Set<UUID> memberIds, UUID actorId) {
        MessageResponse response = mapper.toResponse(message, decryptedContent);
        List<UUID> recipients = memberIds.stream().filter(id -> !id.equals(actorId)).toList();
        webSocketNotifier.notifyUsers(recipients, "NEW_MESSAGE", response);
    }

    private void notifyOthers(Message message, UUID actorId, String type, Object payload) {
        List<UUID> recipients = chatMembershipRepository.findAllByChatId(message.getChatId()).stream()
                .map(ChatMembership::getUserId)
                .filter(id -> actorId == null || !id.equals(actorId))
                .toList();
        webSocketNotifier.notifyUsers(recipients, type, payload);
    }

    private MediaMetadata toMediaMetadata(MediaRequest request) {
        if (request == null) {
            return null;
        }
        return new MediaMetadata(request.url(), request.mimeType(), request.sizeBytes(), request.durationSeconds());
    }
}
