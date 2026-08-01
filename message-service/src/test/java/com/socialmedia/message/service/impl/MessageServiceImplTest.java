package com.socialmedia.message.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.socialmedia.message.client.ChatMembershipClient;
import com.socialmedia.message.domain.ChatMembership;
import com.socialmedia.message.domain.EncryptedEnvelope;
import com.socialmedia.message.domain.Message;
import com.socialmedia.message.domain.MessageType;
import com.socialmedia.message.domain.StarredMessage;
import com.socialmedia.message.dto.request.EditMessageRequest;
import com.socialmedia.message.dto.request.EncryptedEnvelopeRequest;
import com.socialmedia.message.dto.request.SendMessageRequest;
import com.socialmedia.message.event.MessageEventPublisher;
import com.socialmedia.message.exception.NotAChatMemberException;
import com.socialmedia.message.exception.NotMessageAuthorException;
import com.socialmedia.message.mapper.MessageMapper;
import com.socialmedia.message.repository.ChatMembershipRepository;
import com.socialmedia.message.repository.DraftRepository;
import com.socialmedia.message.repository.MessageRepository;
import com.socialmedia.message.repository.StarredMessageRepository;
import com.socialmedia.message.service.EncryptedPayloadValidator;
import com.socialmedia.message.websocket.MessageWebSocketNotifier;
import com.socialmedia.common.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.Base64;
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

    private static final String SENDER_DEVICE = "device-sender";
    private static final String RECIPIENT_DEVICE = "device-recipient";

    @Mock private MessageRepository messageRepository;
    @Mock private DraftRepository draftRepository;
    @Mock private StarredMessageRepository starredMessageRepository;
    @Mock private ChatMembershipRepository chatMembershipRepository;
    @Mock private ChatMembershipClient chatMembershipClient;
    @Mock private MessageEventPublisher eventPublisher;
    @Mock private MessageWebSocketNotifier webSocketNotifier;

    private MessageServiceImpl service;

    @BeforeEach
    void setUp() {
        // The real validator, not a mock - it is pure logic with no key material, and using the
        // real one means these tests also cover that ciphertext actually survives the round trip
        // through it unmodified.
        EncryptedPayloadValidator payloadValidator = new EncryptedPayloadValidator(65536, 64);
        service = new MessageServiceImpl(messageRepository, draftRepository, starredMessageRepository,
                chatMembershipRepository, chatMembershipClient, payloadValidator, eventPublisher,
                webSocketNotifier, new MessageMapper());
        lenient().when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static String cipher(String label) {
        return Base64.getEncoder().encodeToString(("ciphertext-for-" + label).getBytes());
    }

    private static List<EncryptedEnvelopeRequest> envelopesFor(String... deviceIds) {
        return java.util.Arrays.stream(deviceIds)
                .map(d -> new EncryptedEnvelopeRequest(d, 3, cipher(d)))
                .toList();
    }

    private static List<EncryptedEnvelope> storedEnvelopesFor(String... deviceIds) {
        return java.util.Arrays.stream(deviceIds)
                .map(d -> new EncryptedEnvelope(d, 3, cipher(d)))
                .toList();
    }

    @Test
    void sendMessageStoresPerDeviceCiphertextAndPublishesEventWhenNotScheduled() {
        UUID chatId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        String bearer = "Bearer test-token";

        when(chatMembershipClient.fetchMemberIds(chatId, bearer)).thenReturn(Set.of(senderId, recipientId));

        SendMessageRequest request = new SendMessageRequest(MessageType.TEXT,
                envelopesFor(SENDER_DEVICE, RECIPIENT_DEVICE), null, null, null, null);
        var response = service.sendMessage(chatId, senderId, SENDER_DEVICE, bearer, request);

        // The sender gets back only its OWN envelope - never the recipient's.
        assertThat(response.ciphertext()).isEqualTo(cipher(SENDER_DEVICE));
        assertThat(response.cipherType()).isEqualTo(3);
        assertThat(response.sent()).isTrue();
        verify(eventPublisher).publishMessageSent(any(Message.class));
        verify(webSocketNotifier).notifyUsers(eq(List.of(recipientId)), eq("NEW_MESSAGE"), any());
    }

    @Test
    void sendMessageRejectsAPayloadWithNoEnvelopesWithoutCallingChatService() {
        UUID chatId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        String bearer = "Bearer test-token";

        SendMessageRequest request = new SendMessageRequest(MessageType.TEXT, List.of(), null, null, null, null);

        assertThatThrownBy(() -> service.sendMessage(chatId, senderId, SENDER_DEVICE, bearer, request))
                .hasMessageContaining("cannot encrypt on your behalf");
        verify(messageRepository, never()).save(any());
        // Local payload validation must run BEFORE the synchronous chat-service membership call,
        // so an unacceptable request never spends a network round trip or a bulkhead permit.
        verify(chatMembershipClient, never()).fetchMemberIds(any(), anyString());
    }

    @Test
    void sendMessageRejectsMalformedCiphertextWithoutCallingChatService() {
        UUID chatId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        String bearer = "Bearer test-token";

        SendMessageRequest request = new SendMessageRequest(MessageType.TEXT,
                List.of(new EncryptedEnvelopeRequest(RECIPIENT_DEVICE, 3, "!!!not-base64!!!")), null, null, null, null);

        assertThatThrownBy(() -> service.sendMessage(chatId, senderId, SENDER_DEVICE, bearer, request))
                .hasMessageContaining("valid Base64");
        verify(messageRepository, never()).save(any());
        verify(chatMembershipClient, never()).fetchMemberIds(any(), anyString());
    }

    @Test
    void sendMessageRejectsAnUnsupportedCipherTypeWithoutCallingChatService() {
        UUID chatId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        String bearer = "Bearer test-token";

        SendMessageRequest request = new SendMessageRequest(MessageType.TEXT,
                List.of(new EncryptedEnvelopeRequest(RECIPIENT_DEVICE, 99, cipher(RECIPIENT_DEVICE))),
                null, null, null, null);

        assertThatThrownBy(() -> service.sendMessage(chatId, senderId, SENDER_DEVICE, bearer, request))
                .hasMessageContaining("Unsupported cipher type");
        verify(chatMembershipClient, never()).fetchMemberIds(any(), anyString());
    }

    @Test
    void sendMessageRejectedWhenNotAChatMember() {
        UUID chatId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        String bearer = "Bearer test-token";

        when(chatMembershipClient.fetchMemberIds(chatId, bearer)).thenThrow(new NotAChatMemberException());

        SendMessageRequest request = new SendMessageRequest(MessageType.TEXT, envelopesFor(RECIPIENT_DEVICE),
                null, null, null, null);
        assertThatThrownBy(() -> service.sendMessage(chatId, senderId, SENDER_DEVICE, bearer, request))
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
        SendMessageRequest request = new SendMessageRequest(MessageType.TEXT, envelopesFor(SENDER_DEVICE),
                null, null, future, null);
        var response = service.sendMessage(chatId, senderId, SENDER_DEVICE, bearer, request);

        assertThat(response.sent()).isFalse();
        verify(eventPublisher, never()).publishMessageSent(any());
        verify(webSocketNotifier, never()).notifyUsers(any(), anyString(), any());
    }

    @Test
    void readingAMessageOnADeviceItWasNotEncryptedForYieldsNoCiphertext() {
        UUID chatId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        Message message = new Message(chatId, senderId, MessageType.TEXT, storedEnvelopesFor(RECIPIENT_DEVICE),
                null, null, null, null, null);
        when(messageRepository.findById(message.getId())).thenReturn(Optional.of(message));
        when(chatMembershipClient.fetchMemberIds(chatId, "Bearer token")).thenReturn(Set.of(senderId));

        var response = service.getMessage(message.getId(), senderId, "some-other-device", "Bearer token");

        assertThat(response.ciphertext()).isNull();
        assertThat(response.cipherType()).isNull();
    }

    @Test
    void editMessageRejectsNonAuthor() {
        UUID chatId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        Message message = new Message(chatId, senderId, MessageType.TEXT, storedEnvelopesFor(RECIPIENT_DEVICE),
                null, null, null, null, null);
        when(messageRepository.findById(message.getId())).thenReturn(Optional.of(message));

        EditMessageRequest edit = new EditMessageRequest(envelopesFor(RECIPIENT_DEVICE));
        assertThatThrownBy(() -> service.editMessage(message.getId(), otherUserId, "device-x", edit))
                .isInstanceOf(NotMessageAuthorException.class);
    }

    @Test
    void editMessageReplacesEveryEnvelopeWithTheReEncryptedSet() {
        UUID chatId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        Message message = new Message(chatId, senderId, MessageType.TEXT, storedEnvelopesFor(SENDER_DEVICE, RECIPIENT_DEVICE),
                null, null, null, null, null);
        when(messageRepository.findById(message.getId())).thenReturn(Optional.of(message));
        when(chatMembershipRepository.findAllByChatId(chatId)).thenReturn(List.of());

        service.editMessage(message.getId(), senderId, SENDER_DEVICE,
                new EditMessageRequest(List.of(new EncryptedEnvelopeRequest(SENDER_DEVICE, 2, cipher("edited")))));

        assertThat(message.isEdited()).isTrue();
        assertThat(message.getEnvelopes()).hasSize(1);
        assertThat(message.getEnvelopes().get(0).getCiphertext()).isEqualTo(cipher("edited"));
    }

    @Test
    void deleteForEveryoneClearsEveryEnvelopeAndPublishesEvent() {
        UUID chatId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        Message message = new Message(chatId, senderId, MessageType.TEXT, storedEnvelopesFor(SENDER_DEVICE, RECIPIENT_DEVICE),
                null, null, null, null, null);
        when(messageRepository.findById(message.getId())).thenReturn(Optional.of(message));
        when(chatMembershipRepository.findAllByChatId(chatId)).thenReturn(List.of());

        service.deleteMessage(message.getId(), senderId, true);

        assertThat(message.isDeletedForEveryone()).isTrue();
        assertThat(message.getEnvelopes()).isEmpty();
        verify(eventPublisher).publishMessageDeleted(message.getId(), chatId, senderId);
    }

    @Test
    void deleteForMeOnlyHidesFromThatUser() {
        UUID chatId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        Message message = new Message(chatId, senderId, MessageType.TEXT, storedEnvelopesFor(RECIPIENT_DEVICE),
                null, null, null, null, null);
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
        Message message = new Message(chatId, senderId, MessageType.TEXT, storedEnvelopesFor(RECIPIENT_DEVICE),
                null, null, null, null, null);
        when(messageRepository.findById(message.getId())).thenReturn(Optional.of(message));
        when(chatMembershipRepository.existsByChatIdAndUserId(chatId, reactorId)).thenReturn(true);
        when(chatMembershipRepository.findAllByChatId(chatId)).thenReturn(List.of());

        service.reactToMessage(message.getId(), reactorId, RECIPIENT_DEVICE, "👍");
        service.reactToMessage(message.getId(), reactorId, RECIPIENT_DEVICE, "❤️");

        assertThat(message.getReactions()).hasSize(1);
        assertThat(message.getReactions().get(0).getEmoji()).isEqualTo("❤️");
    }

    @Test
    void reactToMessageRejectsNonMemberAccordingToLocalMirror() {
        UUID chatId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID reactorId = UUID.randomUUID();
        Message message = new Message(chatId, senderId, MessageType.TEXT, storedEnvelopesFor(RECIPIENT_DEVICE),
                null, null, null, null, null);
        when(messageRepository.findById(message.getId())).thenReturn(Optional.of(message));
        when(chatMembershipRepository.existsByChatIdAndUserId(chatId, reactorId)).thenReturn(false);

        assertThatThrownBy(() -> service.reactToMessage(message.getId(), reactorId, RECIPIENT_DEVICE, "👍"))
                .isInstanceOf(NotAChatMemberException.class);
    }

    @Test
    void markReadSetsReceiptTimestamps() {
        UUID chatId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID readerId = UUID.randomUUID();
        Message message = new Message(chatId, senderId, MessageType.TEXT, storedEnvelopesFor(RECIPIENT_DEVICE),
                null, null, null, null, null);
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
        Message message = new Message(chatId, senderId, MessageType.TEXT, storedEnvelopesFor(RECIPIENT_DEVICE),
                null, null, null, null, null);
        when(messageRepository.findById(message.getId())).thenReturn(Optional.of(message));
        when(starredMessageRepository.findByUserIdAndMessageId(userId, message.getId())).thenReturn(Optional.empty());
        when(starredMessageRepository.save(any(StarredMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        service.starMessage(message.getId(), userId, RECIPIENT_DEVICE);
        verify(starredMessageRepository).save(any(StarredMessage.class));

        service.unstarMessage(message.getId(), userId);
        verify(starredMessageRepository).deleteByUserIdAndMessageId(userId, message.getId());
    }

    @Test
    void publishDueScheduledMessagesMarksSentAndPublishes() {
        UUID chatId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        Message due = new Message(chatId, senderId, MessageType.TEXT, storedEnvelopesFor(SENDER_DEVICE), null, null, null,
                Instant.now().minusSeconds(5), null);
        when(messageRepository.findAllBySentFalseAndScheduledAtLessThanEqual(any())).thenReturn(List.of(due));
        when(chatMembershipRepository.findAllByChatId(chatId)).thenReturn(
                List.of(new ChatMembership(chatId, senderId)));

        int published = service.publishDueScheduledMessages();

        assertThat(published).isEqualTo(1);
        assertThat(due.isSent()).isTrue();
        verify(eventPublisher).publishMessageSent(due);
    }

    @Test
    void purgeDueSelfDestructMessagesClearsEveryEnvelope() {
        UUID chatId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        Message due = new Message(chatId, senderId, MessageType.TEXT, storedEnvelopesFor(SENDER_DEVICE, RECIPIENT_DEVICE),
                null, null, null, null, Instant.now().minusSeconds(5));
        when(messageRepository.findAllBySelfDestructedFalseAndSelfDestructAtLessThanEqual(any())).thenReturn(List.of(due));
        when(chatMembershipRepository.findAllByChatId(chatId)).thenReturn(List.of());

        int purged = service.purgeDueSelfDestructMessages();

        assertThat(purged).isEqualTo(1);
        assertThat(due.isSelfDestructed()).isTrue();
        assertThat(due.getEnvelopes()).isEmpty();
    }

    @Test
    void getMessageThrowsNotFoundWhenDeletedForTheViewer() {
        UUID chatId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID viewerId = UUID.randomUUID();
        Message message = new Message(chatId, senderId, MessageType.TEXT, storedEnvelopesFor(RECIPIENT_DEVICE),
                null, null, null, null, null);
        message.deleteForUser(viewerId);
        when(messageRepository.findById(message.getId())).thenReturn(Optional.of(message));

        assertThatThrownBy(() -> service.getMessage(message.getId(), viewerId, RECIPIENT_DEVICE, "Bearer token"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(chatMembershipClient, never()).fetchMemberIds(any(), anyString());
    }
}
