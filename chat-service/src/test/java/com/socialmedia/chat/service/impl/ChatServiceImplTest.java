package com.socialmedia.chat.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.socialmedia.chat.domain.Chat;
import com.socialmedia.chat.domain.ChatMember;
import com.socialmedia.chat.domain.ChatMemberRole;
import com.socialmedia.chat.domain.ChatType;
import com.socialmedia.chat.dto.request.CreateGroupChatRequest;
import com.socialmedia.chat.event.ChatEventPublisher;
import com.socialmedia.chat.exception.InsufficientChatPermissionException;
import com.socialmedia.chat.mapper.ChatMapper;
import com.socialmedia.chat.repository.ChatMemberRepository;
import com.socialmedia.chat.repository.ChatRepository;
import com.socialmedia.chat.repository.PinnedMessageRepository;
import com.socialmedia.chat.websocket.ChatWebSocketNotifier;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock private ChatRepository chatRepository;
    @Mock private ChatMemberRepository chatMemberRepository;
    @Mock private PinnedMessageRepository pinnedMessageRepository;
    @Mock private ChatEventPublisher eventPublisher;
    @Mock private ChatWebSocketNotifier webSocketNotifier;

    private ChatServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ChatServiceImpl(chatRepository, chatMemberRepository, pinnedMessageRepository,
                new ChatMapper(), eventPublisher, webSocketNotifier);
        lenient().when(chatMemberRepository.save(any(ChatMember.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createPrivateChatCreatesChatAndTwoMembersWhenNoneExists() {
        UUID requesterId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        UUID chatId = UUID.randomUUID();

        when(chatRepository.findExistingPrivateChatId(requesterId, recipientId)).thenReturn(Optional.empty());
        when(chatRepository.save(any(Chat.class))).thenAnswer(inv -> {
            Chat chat = inv.getArgument(0);
            chat.setId(chatId);
            return chat;
        });

        ChatMember requesterMembership = new ChatMember(chatId, requesterId, ChatMemberRole.MEMBER);
        ChatMember recipientMembership = new ChatMember(chatId, recipientId, ChatMemberRole.MEMBER);
        when(chatMemberRepository.findByChatIdAndUserId(chatId, requesterId)).thenReturn(Optional.of(requesterMembership));
        when(chatMemberRepository.findAllByChatId(chatId)).thenReturn(List.of(requesterMembership, recipientMembership));
        when(chatRepository.findById(chatId)).thenAnswer(inv -> {
            Chat chat = new Chat(ChatType.PRIVATE, null, null, requesterId, false);
            chat.setId(chatId);
            return Optional.of(chat);
        });

        var response = service.createPrivateChat(requesterId, recipientId);

        assertThat(response.type()).isEqualTo(ChatType.PRIVATE);
        assertThat(response.members()).hasSize(2);
        verify(chatMemberRepository, times(2)).save(any(ChatMember.class));
        verify(eventPublisher).publishChatCreated(any(Chat.class), eq(Set.of(requesterId, recipientId)));
        verify(webSocketNotifier).notifyUser(eq(recipientId), eq("CHAT_CREATED"), eq(chatId));
    }

    @Test
    void createPrivateChatReturnsExistingChatInsteadOfDuplicating() {
        UUID requesterId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        UUID existingChatId = UUID.randomUUID();

        when(chatRepository.findExistingPrivateChatId(requesterId, recipientId)).thenReturn(Optional.of(existingChatId));
        Chat existingChat = new Chat(ChatType.PRIVATE, null, null, requesterId, false);
        existingChat.setId(existingChatId);
        when(chatRepository.findById(existingChatId)).thenReturn(Optional.of(existingChat));
        ChatMember requesterMembership = new ChatMember(existingChatId, requesterId, ChatMemberRole.MEMBER);
        when(chatMemberRepository.findByChatIdAndUserId(existingChatId, requesterId)).thenReturn(Optional.of(requesterMembership));
        when(chatMemberRepository.findAllByChatId(existingChatId)).thenReturn(List.of(requesterMembership));

        var response = service.createPrivateChat(requesterId, recipientId);

        assertThat(response.id()).isEqualTo(existingChatId);
        verify(chatRepository, never()).save(any(Chat.class));
        verify(chatMemberRepository, never()).save(any(ChatMember.class));
        verify(eventPublisher, never()).publishChatCreated(any(), any());
    }

    @Test
    void addMembersRejectsNonAdminInAGroupChat() {
        UUID chatId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID newMemberId = UUID.randomUUID();

        Chat groupChat = new Chat(ChatType.GROUP, "Team", null, UUID.randomUUID(), false);
        groupChat.setId(chatId);
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(groupChat));
        when(chatMemberRepository.findByChatIdAndUserId(chatId, requesterId))
                .thenReturn(Optional.of(new ChatMember(chatId, requesterId, ChatMemberRole.MEMBER)));

        assertThatThrownBy(() -> service.addMembers(chatId, requesterId, Set.of(newMemberId)))
                .isInstanceOf(InsufficientChatPermissionException.class);
        verify(chatMemberRepository, never()).save(any(ChatMember.class));
    }

    @Test
    void removeMemberRejectsAdminRemovingTheOwner() {
        UUID chatId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        Chat groupChat = new Chat(ChatType.GROUP, "Team", null, ownerId, false);
        groupChat.setId(chatId);
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(groupChat));
        when(chatMemberRepository.findByChatIdAndUserId(chatId, adminId))
                .thenReturn(Optional.of(new ChatMember(chatId, adminId, ChatMemberRole.ADMIN)));
        when(chatMemberRepository.findByChatIdAndUserId(chatId, ownerId))
                .thenReturn(Optional.of(new ChatMember(chatId, ownerId, ChatMemberRole.OWNER)));

        assertThatThrownBy(() -> service.removeMember(chatId, adminId, ownerId))
                .isInstanceOf(InsufficientChatPermissionException.class);
    }

    @Test
    void markReadResetsUnreadCountForTheCallingMemberOnly() {
        UUID chatId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ChatMember membership = new ChatMember(chatId, userId, ChatMemberRole.MEMBER);
        membership.incrementUnread();
        membership.incrementUnread();
        when(chatMemberRepository.findByChatIdAndUserId(chatId, userId)).thenReturn(Optional.of(membership));

        service.markRead(chatId, userId);

        assertThat(membership.getUnreadCount()).isZero();
    }

    @Test
    void recordIncomingMessageBumpsUnreadCountForEveryoneExceptTheSender() {
        UUID chatId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        Instant sentAt = Instant.parse("2026-07-25T10:00:00Z");

        Chat chat = new Chat(ChatType.PRIVATE, null, null, senderId, false);
        chat.setId(chatId);
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(chatMemberRepository.findAllByChatId(chatId)).thenReturn(List.of(
                new ChatMember(chatId, senderId, ChatMemberRole.MEMBER),
                new ChatMember(chatId, recipientId, ChatMemberRole.MEMBER)));

        service.recordIncomingMessage(chatId, senderId, "Hello there", sentAt);

        assertThat(chat.getLastMessagePreview()).isEqualTo("Hello there");
        assertThat(chat.getLastMessageAt()).isEqualTo(sentAt);
        verify(chatMemberRepository).incrementUnreadForOthers(chatId, senderId);
        verify(webSocketNotifier).notifyUsers(eq(List.of(recipientId)), eq("NEW_MESSAGE"), eq(chatId));
    }

    @Test
    void recordIncomingMessageIgnoresUnknownChatWithoutThrowing() {
        UUID chatId = UUID.randomUUID();
        when(chatRepository.findById(chatId)).thenReturn(Optional.empty());

        service.recordIncomingMessage(chatId, UUID.randomUUID(), "preview", Instant.now());

        verify(chatMemberRepository, never()).incrementUnreadForOthers(any(), any());
        verify(webSocketNotifier, never()).notifyUsers(anyList(), any(), any());
    }

    @Test
    void pinMessageAllowsAnyMemberInAPrivateChatButRequiresAdminInAGroup() {
        UUID privateChatId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        Chat privateChat = new Chat(ChatType.PRIVATE, null, null, userId, false);
        privateChat.setId(privateChatId);
        when(chatRepository.findById(privateChatId)).thenReturn(Optional.of(privateChat));
        when(chatMemberRepository.findByChatIdAndUserId(privateChatId, userId))
                .thenReturn(Optional.of(new ChatMember(privateChatId, userId, ChatMemberRole.MEMBER)));
        when(chatMemberRepository.findAllByChatId(privateChatId)).thenReturn(List.of());
        when(pinnedMessageRepository.findByChatIdAndMessageId(privateChatId, messageId)).thenReturn(Optional.empty());
        when(pinnedMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.pinMessage(privateChatId, userId, messageId);

        assertThat(response.messageId()).isEqualTo(messageId);

        UUID groupChatId = UUID.randomUUID();
        Chat groupChat = new Chat(ChatType.GROUP, "Team", null, UUID.randomUUID(), false);
        groupChat.setId(groupChatId);
        when(chatRepository.findById(groupChatId)).thenReturn(Optional.of(groupChat));
        when(chatMemberRepository.findByChatIdAndUserId(groupChatId, userId))
                .thenReturn(Optional.of(new ChatMember(groupChatId, userId, ChatMemberRole.MEMBER)));

        assertThatThrownBy(() -> service.pinMessage(groupChatId, userId, messageId))
                .isInstanceOf(InsufficientChatPermissionException.class);
    }
}
