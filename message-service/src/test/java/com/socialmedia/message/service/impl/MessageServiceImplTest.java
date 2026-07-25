package com.socialmedia.message.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.socialmedia.message.client.ChatMembershipClient;
import com.socialmedia.message.domain.ChatMembership;
import com.socialmedia.message.domain.Message;
import com.socialmedia.message.domain.MessageType;
import com.socialmedia.message.domain.StarredMessage;
import com.socialmedia.message.dto.request.EditMessageRequest;
import com.socialmedia.message.dto.request.SendMessageRequest;
import com.socialmedia.message.event.MessageEventPublisher;
import com.socialmedia.message.exception.NotAChatMemberException;
import com.socialmedia.message.exception.NotMessageAuthorException;
import com.socialmedia.message.mapper.MessageMapper;
import com.socialmedia.message.repository.ChatMembershipRepository;
import com.socialmedia.message.repository.DraftRepository;
import com.socialmedia.message.repository.MessageRepository;
import com.socialmedia.message.repository.StarredMessageRepository;
import com.socialmedia.message.service.MessageEncryptionService;
import com.socialmedia.message.websocket.MessageWebSocketNotifier;
import com.socialmedia.common.exception.ResourceNotFoundException;
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
class MessageServiceImplTest {

    @Mock private MessageRepository messageRepository;
    @Mock private DraftRepository draftRepository;
    @Mock private StarredMessageRepository starredMessageRepository;
    @Mock private ChatMembershipRepository chatMembershipRepository;
    @Mock private ChatMembershipClient chatMembershipClient;
    @Mock private MessageEncryptionService encryptionService;
    @Mock private MessageEventPublisher eventPublisher;
    @Mock private MessageWebSocketNotifier webSocketNotifier;

    private MessageServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MessageServiceImpl(messageRepository, draftRepository, starredMessageRepository,
                chatMembershipRepository, chatMembershipClient, encryptionService, eventPublisher,
                webSocketNotifier, new MessageMapper());
        lenient().when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(encryptionService.encrypt(anyString())).thenAnswer(inv -> "enc(" + inv.getArgument(0) + ")");
        lenient().when(encryptionService.decrypt(anyString())).thenAnswer(inv -> {
            String value = inv.getArgument(0);
            return value.startsWith("enc(") ? value.substring(4, value.length() - 1) : value;
        });
    }

    @Test
    void sendMessageEncryptsContentAndPublishesEventWhenNotScheduled() {
        UUID chatId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        String bearer = "Bearer test-token";

        when(chatMembershipClient.fetchMemberIds(chatId, bearer)).thenReturn(Set.of(senderId, recipientId));

        SendMessageRequest request = new SendMessageRequest(MessageType.TEXT, "hello", null, null, null, null);
        var response = service.sendMessage(chatId, senderId, bearer, request);

        assertThat(response.content()).isEqualTo("hello");
        assertThat(response.sent()).isTrue();
        verify(eventPublisher).publishMessageSent(any(Message.class), eq("hello"));
        verify(webSocketNotifier).notifyUsers(eq(List.of(recipientId)), eq("NEW_MESSAGE"), any());
    }

    @Test
    void sendMessageRejectedWhenNotAChatMember() {
        UUID chatId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        String bearer = "Bearer test-token";

        when(chatMembershipClient.fetchMemberIds(chatId, bearer)).thenThrow(new NotAChatMemberException());

        SendMessageRequest request = new SendMessageRequest(MessageType.TEXT, "hello", null, null, null, null);
        assertThatThrownBy(() -> service.sendMessage(chatId, senderId, bearer, request))
                .isInstanceOf(NotAChatMemberException.class);
        verify(messageRepository, never()).save(any());
    }

    @Test
    void sendMessageWithFutureScheduledAtIsStoredButNotPublishedYet() {
        UUID chatId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        String bearer = "Bearer test-token";
        when(chatMembershipClient.fetchMemberIds(chatId, bearer)).thenReturn(Set.of(senderId));

        Instant future = Instant.now().plusSeconds(3600);
        SendMessageRequest request = new SendMessageRequest(MessageType.TEXT, "later", null, null, future, null);
        var response = service.sendMessage(chatId, senderId, bearer, request);

        assertThat(response.sent()).isFalse();
        verify(eventPublisher, never()).publishMessageSent(any(), any());
        verify(webSocketNotifier, never()).notifyUsers(any(), anyString(), any());
    }

    @Test
    void editMessageRejectsNonAuthor() {
        UUID chatId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        Message message = new Message(chatId, senderId, MessageType.TEXT, "enc(hi)", null, null, null, null, null);
        when(messageRepository.findById(message.getId())).thenReturn(Optional.of(message));

        assertThatThrownBy(() -> service.editMessage(message.getId(), otherUserId, new EditMessageRequest("nope")))
                .isInstanceOf(NotMessageAuthorException.class);
    }

    @Test
    void deleteForEveryoneClearsContentAndPublishesEvent() {
        UUID chatId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        Message message = new Message(chatId, senderId, MessageType.TEXT, "enc(secret)", null, null, null, null, null);
        when(messageRepository.findById(message.getId())).thenReturn(Optional.of(message));
        when(chatMembershipRepository.findAllByChatId(chatId)).thenReturn(List.of());

        service.deleteMessage(message.getId(), senderId, true);

        assertThat(message.isDeletedForEveryone()).isTrue();
        assertThat(message.getContent()).isNull();
        verify(eventPublisher).publishMessageDeleted(message.getId(), chatId, senderId);
    }

    @Test
    void deleteForMeOnlyHidesFromThatUser() {
        UUID chatId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        Message message = new Message(chatId, senderId, MessageType.TEXT, "enc(secret)", null, null, null, null, null);
        when(messageRepository.findById(message.getId())).thenReturn(Optional.of(message));

        service.deleteMessage(message.getId(), otherUserId, false);

        assertThat(message.isDeletedForEveryone()).isFalse();
        assertThat(message.getDeletedForUserIds()).containsExactly(otherUserId);
        verify(eventPublisher, never()).publishMessageDeleted(any(), any(), any());
    }

    @Test
    void reactToMessageReplacesThePreviousEmojiFromTheSameUser() {
        UUID chatId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID reactorId = UUID.randomUUID();
        Message message = new Message(chatId, senderId, MessageType.TEXT, "enc(hi)", null, null, null, null, null);
        when(messageRepository.findById(message.getId())).thenReturn(Optional.of(message));
        when(chatMembershipRepository.existsByChatIdAndUserId(chatId, reactorId)).thenReturn(true);
        when(chatMembershipRepository.findAllByChatId(chatId)).thenReturn(List.of());

        service.reactToMessage(message.getId(), reactorId, "👍");
        service.reactToMessage(message.getId(), reactorId, "❤️");

        assertThat(message.getReactions()).hasSize(1);
        assertThat(message.getReactions().get(0).getEmoji()).isEqualTo("❤️");
    }

    @Test
    void reactToMessageRejectsNonMemberAccordingToLocalMirror() {
        UUID chatId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID reactorId = UUID.randomUUID();
        Message message = new Message(chatId, senderId, MessageType.TEXT, "enc(hi)", null, null, null, null, null);
        when(messageRepository.findById(message.getId())).thenReturn(Optional.of(message));
        when(chatMembershipRepository.existsByChatIdAndUserId(chatId, reactorId)).thenReturn(false);

        assertThatThrownBy(() -> service.reactToMessage(message.getId(), reactorId, "👍"))
                .isInstanceOf(NotAChatMemberException.class);
    }

    @Test
    void markReadSetsReceiptTimestamps() {
        UUID chatId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID readerId = UUID.randomUUID();
        Message message = new Message(chatId, senderId, MessageType.TEXT, "enc(hi)", null, null, null, null, null);
        when(messageRepository.findById(message.getId())).thenReturn(Optional.of(message));
        when(chatMembershipRepository.existsByChatIdAndUserId(chatId, readerId)).thenReturn(true);
        when(chatMembershipRepository.findAllByChatId(chatId)).thenReturn(List.of());

        service.markRead(message.getId(), readerId);

        assertThat(message.receiptFor(readerId).getReadAt()).isNotNull();
        assertThat(message.receiptFor(readerId).getDeliveredAt()).isNotNull();
    }

    @Test
    void starAndUnstarMessage() {
        UUID chatId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Message message = new Message(chatId, senderId, MessageType.TEXT, "enc(hi)", null, null, null, null, null);
        when(messageRepository.findById(message.getId())).thenReturn(Optional.of(message));
        when(starredMessageRepository.findByUserIdAndMessageId(userId, message.getId())).thenReturn(Optional.empty());
        when(starredMessageRepository.save(any(StarredMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        service.starMessage(message.getId(), userId);
        verify(starredMessageRepository).save(any(StarredMessage.class));

        service.unstarMessage(message.getId(), userId);
        verify(starredMessageRepository).deleteByUserIdAndMessageId(userId, message.getId());
    }

    @Test
    void publishDueScheduledMessagesMarksSentAndPublishes() {
        UUID chatId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        Message due = new Message(chatId, senderId, MessageType.TEXT, "enc(later)", null, null, null,
                Instant.now().minusSeconds(5), null);
        when(messageRepository.findAllBySentFalseAndScheduledAtLessThanEqual(any())).thenReturn(List.of(due));
        when(chatMembershipRepository.findAllByChatId(chatId)).thenReturn(
                List.of(new ChatMembership(chatId, senderId)));

        int published = service.publishDueScheduledMessages();

        assertThat(published).isEqualTo(1);
        assertThat(due.isSent()).isTrue();
        verify(eventPublisher).publishMessageSent(due, "later");
    }

    @Test
    void purgeDueSelfDestructMessagesClearsContent() {
        UUID chatId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        Message due = new Message(chatId, senderId, MessageType.TEXT, "enc(secret)", null, null, null, null,
                Instant.now().minusSeconds(5));
        when(messageRepository.findAllBySelfDestructedFalseAndSelfDestructAtLessThanEqual(any())).thenReturn(List.of(due));
        when(chatMembershipRepository.findAllByChatId(chatId)).thenReturn(List.of());

        int purged = service.purgeDueSelfDestructMessages();

        assertThat(purged).isEqualTo(1);
        assertThat(due.isSelfDestructed()).isTrue();
        assertThat(due.getContent()).isNull();
    }

    @Test
    void getMessageThrowsNotFoundWhenDeletedForTheViewer() {
        UUID chatId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID viewerId = UUID.randomUUID();
        Message message = new Message(chatId, senderId, MessageType.TEXT, "enc(hi)", null, null, null, null, null);
        message.deleteForUser(viewerId);
        when(messageRepository.findById(message.getId())).thenReturn(Optional.of(message));

        assertThatThrownBy(() -> service.getMessage(message.getId(), viewerId, "Bearer token"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(chatMembershipClient, never()).fetchMemberIds(any(), anyString());
    }
}
