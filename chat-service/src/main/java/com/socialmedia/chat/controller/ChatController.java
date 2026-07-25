package com.socialmedia.chat.controller;

import com.socialmedia.chat.dto.request.AddMembersRequest;
import com.socialmedia.chat.dto.request.ChangeRoleRequest;
import com.socialmedia.chat.dto.request.CreateGroupChatRequest;
import com.socialmedia.chat.dto.request.CreatePrivateChatRequest;
import com.socialmedia.chat.dto.request.MuteChatRequest;
import com.socialmedia.chat.dto.request.PinMessageRequest;
import com.socialmedia.chat.dto.request.UpdateChatRequest;
import com.socialmedia.chat.dto.response.ChatDetailResponse;
import com.socialmedia.chat.dto.response.ChatSummaryResponse;
import com.socialmedia.chat.dto.response.PinnedMessageResponse;
import com.socialmedia.chat.service.ChatService;
import com.socialmedia.common.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chats")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/private")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatDetailResponse createPrivateChat(@Valid @RequestBody CreatePrivateChatRequest request) {
        return chatService.createPrivateChat(CurrentUser.get().userId(), request.recipientUserId());
    }

    @PostMapping("/group")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatDetailResponse createGroupChat(@Valid @RequestBody CreateGroupChatRequest request) {
        return chatService.createGroupChat(CurrentUser.get().userId(), request);
    }

    @GetMapping
    public List<ChatSummaryResponse> listMyChats(@RequestParam(defaultValue = "false") boolean includeArchived) {
        return chatService.listMyChats(CurrentUser.get().userId(), includeArchived);
    }

    @GetMapping("/search")
    public List<ChatSummaryResponse> searchMyChats(@RequestParam String q) {
        return chatService.searchMyChats(CurrentUser.get().userId(), q);
    }

    @GetMapping("/{chatId}")
    public ChatDetailResponse getChat(@PathVariable UUID chatId) {
        return chatService.getChat(chatId, CurrentUser.get().userId());
    }

    @PatchMapping("/{chatId}")
    public ChatDetailResponse updateChat(@PathVariable UUID chatId, @Valid @RequestBody UpdateChatRequest request) {
        return chatService.updateChat(chatId, CurrentUser.get().userId(), request);
    }

    @DeleteMapping("/{chatId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteChat(@PathVariable UUID chatId) {
        chatService.deleteChat(chatId, CurrentUser.get().userId());
    }

    @PostMapping("/{chatId}/members")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addMembers(@PathVariable UUID chatId, @Valid @RequestBody AddMembersRequest request) {
        chatService.addMembers(chatId, CurrentUser.get().userId(), request.userIds());
    }

    @DeleteMapping("/{chatId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable UUID chatId, @PathVariable UUID userId) {
        chatService.removeMember(chatId, CurrentUser.get().userId(), userId);
    }

    @PostMapping("/{chatId}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leaveChat(@PathVariable UUID chatId) {
        UUID selfId = CurrentUser.get().userId();
        chatService.removeMember(chatId, selfId, selfId);
    }

    @PatchMapping("/{chatId}/members/{userId}/role")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeRole(@PathVariable UUID chatId, @PathVariable UUID userId, @Valid @RequestBody ChangeRoleRequest request) {
        chatService.changeRole(chatId, CurrentUser.get().userId(), userId, request.role());
    }

    @PostMapping("/{chatId}/mute")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void muteChat(@PathVariable UUID chatId, @RequestBody(required = false) MuteChatRequest request) {
        chatService.muteChat(chatId, CurrentUser.get().userId(), request == null ? null : request.mutedUntil());
    }

    @DeleteMapping("/{chatId}/mute")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unmuteChat(@PathVariable UUID chatId) {
        chatService.unmuteChat(chatId, CurrentUser.get().userId());
    }

    @PostMapping("/{chatId}/archive")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archiveChat(@PathVariable UUID chatId) {
        chatService.archiveChat(chatId, CurrentUser.get().userId());
    }

    @DeleteMapping("/{chatId}/archive")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unarchiveChat(@PathVariable UUID chatId) {
        chatService.unarchiveChat(chatId, CurrentUser.get().userId());
    }

    @PostMapping("/{chatId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable UUID chatId) {
        chatService.markRead(chatId, CurrentUser.get().userId());
    }

    @PostMapping("/{chatId}/pinned-messages")
    @ResponseStatus(HttpStatus.CREATED)
    public PinnedMessageResponse pinMessage(@PathVariable UUID chatId, @Valid @RequestBody PinMessageRequest request) {
        return chatService.pinMessage(chatId, CurrentUser.get().userId(), request.messageId());
    }

    @GetMapping("/{chatId}/pinned-messages")
    public List<PinnedMessageResponse> listPinnedMessages(@PathVariable UUID chatId) {
        return chatService.listPinnedMessages(chatId, CurrentUser.get().userId());
    }

    @DeleteMapping("/{chatId}/pinned-messages/{messageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unpinMessage(@PathVariable UUID chatId, @PathVariable UUID messageId) {
        chatService.unpinMessage(chatId, CurrentUser.get().userId(), messageId);
    }
}
