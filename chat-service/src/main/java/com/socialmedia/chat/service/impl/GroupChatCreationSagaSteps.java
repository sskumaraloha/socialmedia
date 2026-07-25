package com.socialmedia.chat.service.impl;

import com.socialmedia.chat.domain.Chat;
import com.socialmedia.chat.domain.ChatMember;
import com.socialmedia.chat.domain.ChatMemberRole;
import com.socialmedia.chat.domain.ChatType;
import com.socialmedia.chat.dto.request.CreateGroupChatRequest;
import com.socialmedia.chat.event.ChatEventPublisher;
import com.socialmedia.chat.repository.ChatMemberRepository;
import com.socialmedia.chat.repository.ChatRepository;
import com.socialmedia.chat.repository.PinnedMessageRepository;
import com.socialmedia.chat.websocket.ChatWebSocketNotifier;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The individually-committing local steps of GroupChatCreationSaga. Each method runs in its
 * own new transaction (REQUIRES_NEW) rather than inheriting the caller's - a saga step must
 * commit (or roll back) independently of the orchestrator's non-transactional external call
 * in between, otherwise "create locally, then validate externally, then compensate" would
 * either hold a database transaction open across a network call, or - since Spring only
 * proxies calls that arrive through the bean, not internal self-invocation - silently not be
 * transactional at all. Must be called through the injected bean (never `this.step(...)`
 * from another method on the same class) for the proxy, and therefore @Transactional, to
 * apply.
 */
@Component
public class GroupChatCreationSagaSteps {

    private final ChatRepository chatRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final PinnedMessageRepository pinnedMessageRepository;
    private final ChatEventPublisher eventPublisher;
    private final ChatWebSocketNotifier webSocketNotifier;

    public GroupChatCreationSagaSteps(ChatRepository chatRepository, ChatMemberRepository chatMemberRepository,
            PinnedMessageRepository pinnedMessageRepository, ChatEventPublisher eventPublisher,
            ChatWebSocketNotifier webSocketNotifier) {
        this.chatRepository = chatRepository;
        this.chatMemberRepository = chatMemberRepository;
        this.pinnedMessageRepository = pinnedMessageRepository;
        this.eventPublisher = eventPublisher;
        this.webSocketNotifier = webSocketNotifier;
    }

    /** Saga step 1 (local, commits on return): optimistically create the chat and every
     * requested membership before any invited user has been validated. Nothing external-
     * facing happens yet - no event is published, no one is notified - so if step 2 fails,
     * compensating is just deleting rows nobody outside this transaction has seen. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public StepResult createLocally(UUID requesterId, CreateGroupChatRequest request) {
        ChatType type = request.channel() ? ChatType.CHANNEL : ChatType.GROUP;
        Chat chat = new Chat(type, request.name(), request.description(), requesterId, request.broadcastOnly());
        chatRepository.save(chat);
        chatMemberRepository.save(new ChatMember(chat.getId(), requesterId, ChatMemberRole.OWNER));

        Set<UUID> invitedMemberIds = new LinkedHashSet<>(request.memberIds());
        invitedMemberIds.remove(requesterId);
        for (UUID memberId : invitedMemberIds) {
            chatMemberRepository.save(new ChatMember(chat.getId(), memberId, ChatMemberRole.MEMBER));
        }

        Set<UUID> allMemberIds = new LinkedHashSet<>(invitedMemberIds);
        allMemberIds.add(requesterId);
        return new StepResult(chat.getId(), invitedMemberIds, allMemberIds);
    }

    /** Saga step 3 (local, commits on return): every invited member checked out, so make the
     * chat externally visible - publish the creation event (via the outbox, in this same
     * transaction) and notify the new members over WebSocket. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void confirm(UUID chatId, UUID requesterId, Set<UUID> allMemberIds) {
        Chat chat = chatRepository.findById(chatId).orElseThrow();
        eventPublisher.publishChatCreated(chat, allMemberIds);
        webSocketNotifier.notifyUsers(allMemberIds.stream().filter(id -> !id.equals(requesterId)).toList(),
                "CHAT_CREATED", chatId);
    }

    /** Compensating transaction: step 2 found an invalid/unreachable invited member, so undo
     * step 1 entirely. Safe to run even though nothing was ever published - the chat was
     * never confirmed, so no consumer, no other service, and no other member ever learned
     * it existed. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void compensate(UUID chatId) {
        pinnedMessageRepository.deleteByChatId(chatId);
        chatMemberRepository.deleteByChatId(chatId);
        chatRepository.deleteById(chatId);
    }

    public record StepResult(UUID chatId, Set<UUID> invitedMemberIds, Set<UUID> allMemberIds) {
    }
}
