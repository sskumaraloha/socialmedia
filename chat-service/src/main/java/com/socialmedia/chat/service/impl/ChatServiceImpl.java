package com.socialmedia.chat.service.impl;

import com.socialmedia.chat.domain.Chat;
import com.socialmedia.chat.domain.ChatMember;
import com.socialmedia.chat.domain.ChatMemberRole;
import com.socialmedia.chat.domain.ChatType;
import com.socialmedia.chat.domain.PinnedMessage;
import com.socialmedia.chat.dto.request.CreateGroupChatRequest;
import com.socialmedia.chat.dto.request.UpdateChatRequest;
import com.socialmedia.chat.dto.response.ChatDetailResponse;
import com.socialmedia.chat.dto.response.ChatSummaryResponse;
import com.socialmedia.chat.dto.response.PinnedMessageResponse;
import com.socialmedia.chat.event.ChatEventPublisher;
import com.socialmedia.chat.exception.InsufficientChatPermissionException;
import com.socialmedia.chat.exception.NotAChatMemberException;
import com.socialmedia.chat.mapper.ChatMapper;
import com.socialmedia.chat.repository.ChatMemberRepository;
import com.socialmedia.chat.repository.ChatRepository;
import com.socialmedia.chat.repository.PinnedMessageRepository;
import com.socialmedia.chat.service.ChatService;
import com.socialmedia.chat.websocket.ChatWebSocketNotifier;
import com.socialmedia.common.exception.BusinessException;
import com.socialmedia.common.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ChatServiceImpl implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);

    private final ChatRepository chatRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final PinnedMessageRepository pinnedMessageRepository;
    private final ChatMapper chatMapper;
    private final ChatEventPublisher eventPublisher;
    private final ChatWebSocketNotifier webSocketNotifier;

    public ChatServiceImpl(ChatRepository chatRepository, ChatMemberRepository chatMemberRepository,
            PinnedMessageRepository pinnedMessageRepository, ChatMapper chatMapper,
            ChatEventPublisher eventPublisher, ChatWebSocketNotifier webSocketNotifier) {
        this.chatRepository = chatRepository;
        this.chatMemberRepository = chatMemberRepository;
        this.pinnedMessageRepository = pinnedMessageRepository;
        this.chatMapper = chatMapper;
        this.eventPublisher = eventPublisher;
        this.webSocketNotifier = webSocketNotifier;
    }

    @Override
    public ChatDetailResponse createPrivateChat(UUID requesterId, UUID recipientId) {
        if (requesterId.equals(recipientId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "CANNOT_CHAT_WITH_SELF", "Cannot start a private chat with yourself");
        }
        UUID existingChatId = chatRepository.findExistingPrivateChatId(requesterId, recipientId).orElse(null);
        if (existingChatId != null) {
            return getChat(existingChatId, requesterId);
        }

        Chat chat = new Chat(ChatType.PRIVATE, null, null, requesterId, false);
        chatRepository.save(chat);
        chatMemberRepository.save(new ChatMember(chat.getId(), requesterId, ChatMemberRole.MEMBER));
        chatMemberRepository.save(new ChatMember(chat.getId(), recipientId, ChatMemberRole.MEMBER));

        eventPublisher.publishChatCreated(chat, Set.of(requesterId, recipientId));
        webSocketNotifier.notifyUser(recipientId, "CHAT_CREATED", chat.getId());

        return getChat(chat.getId(), requesterId);
    }

    @Override
    public ChatDetailResponse createGroupChat(UUID requesterId, CreateGroupChatRequest request) {
        ChatType type = request.channel() ? ChatType.CHANNEL : ChatType.GROUP;
        Chat chat = new Chat(type, request.name(), request.description(), requesterId, request.broadcastOnly());
        chatRepository.save(chat);
        chatMemberRepository.save(new ChatMember(chat.getId(), requesterId, ChatMemberRole.OWNER));

        Set<UUID> allMemberIds = new LinkedHashSet<>(request.memberIds());
        allMemberIds.remove(requesterId);
        for (UUID memberId : allMemberIds) {
            chatMemberRepository.save(new ChatMember(chat.getId(), memberId, ChatMemberRole.MEMBER));
        }
        allMemberIds.add(requesterId);

        eventPublisher.publishChatCreated(chat, allMemberIds);
        webSocketNotifier.notifyUsers(allMemberIds.stream().filter(id -> !id.equals(requesterId)).toList(),
                "CHAT_CREATED", chat.getId());

        return getChat(chat.getId(), requesterId);
    }

    @Override
    @Transactional(readOnly = true)
    public ChatDetailResponse getChat(UUID chatId, UUID viewerId) {
        Chat chat = requireChat(chatId);
        ChatMember viewerMembership = requireMember(chatId, viewerId);
        List<ChatMember> allMembers = chatMemberRepository.findAllByChatId(chatId);
        return chatMapper.toDetail(chat, viewerMembership, allMembers);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatSummaryResponse> listMyChats(UUID viewerId, boolean includeArchived) {
        return chatMemberRepository.findAllByUserId(viewerId).stream()
                .filter(m -> includeArchived || !m.isArchived())
                .map(m -> chatMapper.toSummary(requireChat(m.getChatId()), m))
                .sorted((a, b) -> {
                    Instant left = a.lastMessageAt();
                    Instant right = b.lastMessageAt();
                    if (left == null && right == null) {
                        return 0;
                    }
                    if (left == null) {
                        return 1;
                    }
                    if (right == null) {
                        return -1;
                    }
                    return right.compareTo(left);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatSummaryResponse> searchMyChats(UUID viewerId, String query) {
        String needle = query.toLowerCase();
        return chatMemberRepository.findAllByUserId(viewerId).stream()
                .map(m -> new Object[] {requireChat(m.getChatId()), m})
                .filter(pair -> {
                    Chat chat = (Chat) pair[0];
                    return chat.getName() != null && chat.getName().toLowerCase().contains(needle);
                })
                .map(pair -> chatMapper.toSummary((Chat) pair[0], (ChatMember) pair[1]))
                .toList();
    }

    @Override
    public ChatDetailResponse updateChat(UUID chatId, UUID requesterId, UpdateChatRequest request) {
        Chat chat = requireChat(chatId);
        if (chat.getType() == ChatType.PRIVATE) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "PRIVATE_CHAT_NOT_EDITABLE", "Private chats cannot be renamed");
        }
        requireAdmin(chatId, requesterId, "edit this chat");

        if (request.name() != null) {
            chat.setName(request.name());
        }
        if (request.description() != null) {
            chat.setDescription(request.description());
        }
        if (request.avatarUrl() != null) {
            chat.setAvatarUrl(request.avatarUrl());
        }

        eventPublisher.publishChatUpdated(chat);
        List<UUID> otherMembers = chatMemberRepository.findAllByChatId(chatId).stream()
                .map(ChatMember::getUserId).filter(id -> !id.equals(requesterId)).toList();
        webSocketNotifier.notifyUsers(otherMembers, "CHAT_UPDATED", chatId);

        return getChat(chatId, requesterId);
    }

    @Override
    public void deleteChat(UUID chatId, UUID requesterId) {
        Chat chat = requireChat(chatId);
        ChatMember requesterMembership = requireMember(chatId, requesterId);
        if (chat.getType() != ChatType.PRIVATE && requesterMembership.getRole() != ChatMemberRole.OWNER) {
            throw new InsufficientChatPermissionException("delete this chat");
        }

        List<UUID> memberIds = chatMemberRepository.findAllByChatId(chatId).stream().map(ChatMember::getUserId).toList();
        pinnedMessageRepository.deleteByChatId(chatId);
        chatMemberRepository.deleteByChatId(chatId);
        chatRepository.delete(chat);

        eventPublisher.publishChatDeleted(chatId, requesterId);
        webSocketNotifier.notifyUsers(memberIds.stream().filter(id -> !id.equals(requesterId)).toList(), "CHAT_DELETED", chatId);
    }

    @Override
    public void addMembers(UUID chatId, UUID requesterId, Set<UUID> userIds) {
        Chat chat = requireChat(chatId);
        if (chat.getType() == ChatType.PRIVATE) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "PRIVATE_CHAT_FIXED_MEMBERSHIP", "Cannot add members to a private chat");
        }
        requireAdmin(chatId, requesterId, "add members");

        for (UUID userId : userIds) {
            if (!chatMemberRepository.existsByChatIdAndUserId(chatId, userId)) {
                chatMemberRepository.save(new ChatMember(chatId, userId, ChatMemberRole.MEMBER));
                eventPublisher.publishMemberAdded(chatId, userId, requesterId);
                webSocketNotifier.notifyUser(userId, "ADDED_TO_CHAT", chatId);
            }
        }
    }

    @Override
    public void removeMember(UUID chatId, UUID requesterId, UUID targetUserId) {
        Chat chat = requireChat(chatId);
        ChatMember requesterMembership = requireMember(chatId, requesterId);
        ChatMember targetMembership = chatMemberRepository.findByChatIdAndUserId(chatId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat member", targetUserId));

        boolean isSelfRemoval = requesterId.equals(targetUserId);
        if (!isSelfRemoval) {
            if (chat.getType() == ChatType.PRIVATE) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "PRIVATE_CHAT_FIXED_MEMBERSHIP", "Cannot remove members from a private chat");
            }
            if (!requesterMembership.getRole().hasAdminPrivileges()) {
                throw new InsufficientChatPermissionException("remove members");
            }
            if (targetMembership.getRole() == ChatMemberRole.OWNER
                    || (targetMembership.getRole() == ChatMemberRole.ADMIN && requesterMembership.getRole() != ChatMemberRole.OWNER)) {
                throw new InsufficientChatPermissionException("remove a member with equal or higher privileges");
            }
        }

        chatMemberRepository.delete(targetMembership);
        eventPublisher.publishMemberRemoved(chatId, targetUserId, requesterId);
        if (!isSelfRemoval) {
            webSocketNotifier.notifyUser(targetUserId, "REMOVED_FROM_CHAT", chatId);
        }
    }

    @Override
    public void changeRole(UUID chatId, UUID requesterId, UUID targetUserId, ChatMemberRole newRole) {
        requireChat(chatId);
        ChatMember requesterMembership = requireMember(chatId, requesterId);
        if (requesterMembership.getRole() != ChatMemberRole.OWNER) {
            throw new InsufficientChatPermissionException("change member roles");
        }
        ChatMember targetMembership = chatMemberRepository.findByChatIdAndUserId(chatId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat member", targetUserId));

        if (newRole == ChatMemberRole.OWNER) {
            requesterMembership.setRole(ChatMemberRole.ADMIN);
        }
        targetMembership.setRole(newRole);
        webSocketNotifier.notifyUser(targetUserId, "ROLE_CHANGED", chatId);
    }

    @Override
    public void muteChat(UUID chatId, UUID requesterId, Instant mutedUntil) {
        requireMember(chatId, requesterId).mute(mutedUntil);
    }

    @Override
    public void unmuteChat(UUID chatId, UUID requesterId) {
        requireMember(chatId, requesterId).unmute();
    }

    @Override
    public void archiveChat(UUID chatId, UUID requesterId) {
        requireMember(chatId, requesterId).setArchived(true);
    }

    @Override
    public void unarchiveChat(UUID chatId, UUID requesterId) {
        requireMember(chatId, requesterId).setArchived(false);
    }

    @Override
    public void markRead(UUID chatId, UUID requesterId) {
        requireMember(chatId, requesterId).markRead();
    }

    @Override
    public PinnedMessageResponse pinMessage(UUID chatId, UUID requesterId, UUID messageId) {
        Chat chat = requireChat(chatId);
        ChatMember requesterMembership = requireMember(chatId, requesterId);
        if (chat.getType() != ChatType.PRIVATE && !requesterMembership.getRole().hasAdminPrivileges()) {
            throw new InsufficientChatPermissionException("pin messages");
        }

        PinnedMessage pinned = pinnedMessageRepository.findByChatIdAndMessageId(chatId, messageId)
                .orElseGet(() -> pinnedMessageRepository.save(new PinnedMessage(chatId, messageId, requesterId)));

        eventPublisher.publishMessagePinned(chatId, messageId, requesterId);
        List<UUID> otherMembers = chatMemberRepository.findAllByChatId(chatId).stream()
                .map(ChatMember::getUserId).filter(id -> !id.equals(requesterId)).toList();
        webSocketNotifier.notifyUsers(otherMembers, "MESSAGE_PINNED", messageId);

        return chatMapper.toPinnedResponse(pinned);
    }

    @Override
    public void unpinMessage(UUID chatId, UUID requesterId, UUID messageId) {
        Chat chat = requireChat(chatId);
        ChatMember requesterMembership = requireMember(chatId, requesterId);
        if (chat.getType() != ChatType.PRIVATE && !requesterMembership.getRole().hasAdminPrivileges()) {
            throw new InsufficientChatPermissionException("unpin messages");
        }
        pinnedMessageRepository.deleteByChatIdAndMessageId(chatId, messageId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PinnedMessageResponse> listPinnedMessages(UUID chatId, UUID viewerId) {
        requireMember(chatId, viewerId);
        return pinnedMessageRepository.findAllByChatIdOrderByCreatedAtDesc(chatId).stream()
                .map(chatMapper::toPinnedResponse)
                .toList();
    }

    @Override
    public void recordIncomingMessage(UUID chatId, UUID senderId, String contentPreview, Instant sentAt) {
        Chat chat = chatRepository.findById(chatId).orElse(null);
        if (chat == null) {
            log.warn("Received message.sent event for unknown chat {}", chatId);
            return;
        }
        chat.recordIncomingMessage(sentAt, contentPreview);
        chatMemberRepository.incrementUnreadForOthers(chatId, senderId);

        List<UUID> recipients = chatMemberRepository.findAllByChatId(chatId).stream()
                .map(ChatMember::getUserId).filter(id -> !id.equals(senderId)).toList();
        webSocketNotifier.notifyUsers(recipients, "NEW_MESSAGE", chatId);
    }

    private Chat requireChat(UUID chatId) {
        return chatRepository.findById(chatId).orElseThrow(() -> new ResourceNotFoundException("Chat", chatId));
    }

    private ChatMember requireMember(UUID chatId, UUID userId) {
        return chatMemberRepository.findByChatIdAndUserId(chatId, userId).orElseThrow(NotAChatMemberException::new);
    }

    private void requireAdmin(UUID chatId, UUID userId, String action) {
        ChatMember membership = requireMember(chatId, userId);
        if (!membership.getRole().hasAdminPrivileges()) {
            throw new InsufficientChatPermissionException(action);
        }
    }
}
