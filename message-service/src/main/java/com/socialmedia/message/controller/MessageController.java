package com.socialmedia.message.controller;

import com.socialmedia.message.dto.request.DraftRequest;
import com.socialmedia.message.dto.request.EditMessageRequest;
import com.socialmedia.message.dto.request.ForwardMessageRequest;
import com.socialmedia.message.dto.request.ReactionRequest;
import com.socialmedia.message.dto.request.SendMessageRequest;
import com.socialmedia.message.dto.response.DraftResponse;
import com.socialmedia.message.dto.response.MessageResponse;
import com.socialmedia.message.service.MessageService;
import com.socialmedia.common.security.CurrentUser;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping("/api/v1/chats/{chatId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse sendMessage(@PathVariable UUID chatId, @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody SendMessageRequest request) {
        return messageService.sendMessage(chatId, CurrentUser.get().userId(), authorization, request);
    }

    @GetMapping("/api/v1/chats/{chatId}/messages")
    public List<MessageResponse> listMessages(@PathVariable UUID chatId, @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) Instant before, @RequestParam(defaultValue = "50") int limit) {
        return messageService.listMessages(chatId, CurrentUser.get().userId(), authorization, before, limit);
    }

    @GetMapping("/api/v1/messages/{messageId}")
    public MessageResponse getMessage(@PathVariable UUID messageId, @RequestHeader("Authorization") String authorization) {
        return messageService.getMessage(messageId, CurrentUser.get().userId(), authorization);
    }

    @PatchMapping("/api/v1/messages/{messageId}")
    public MessageResponse editMessage(@PathVariable UUID messageId, @Valid @RequestBody EditMessageRequest request) {
        return messageService.editMessage(messageId, CurrentUser.get().userId(), request);
    }

    @DeleteMapping("/api/v1/messages/{messageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMessage(@PathVariable UUID messageId, @RequestParam(defaultValue = "false") boolean forEveryone) {
        messageService.deleteMessage(messageId, CurrentUser.get().userId(), forEveryone);
    }

    @PostMapping("/api/v1/messages/{messageId}/forward")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse forwardMessage(@PathVariable UUID messageId, @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody ForwardMessageRequest request) {
        return messageService.forwardMessage(messageId, CurrentUser.get().userId(), authorization, request.targetChatId());
    }

    @PutMapping("/api/v1/messages/{messageId}/reactions")
    public MessageResponse reactToMessage(@PathVariable UUID messageId, @Valid @RequestBody ReactionRequest request) {
        return messageService.reactToMessage(messageId, CurrentUser.get().userId(), request.emoji());
    }

    @DeleteMapping("/api/v1/messages/{messageId}/reactions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeReaction(@PathVariable UUID messageId) {
        messageService.removeReaction(messageId, CurrentUser.get().userId());
    }

    @PostMapping("/api/v1/messages/{messageId}/delivered")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markDelivered(@PathVariable UUID messageId) {
        messageService.markDelivered(messageId, CurrentUser.get().userId());
    }

    @PostMapping("/api/v1/messages/{messageId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable UUID messageId) {
        messageService.markRead(messageId, CurrentUser.get().userId());
    }

    @PutMapping("/api/v1/messages/{messageId}/star")
    public MessageResponse starMessage(@PathVariable UUID messageId) {
        return messageService.starMessage(messageId, CurrentUser.get().userId());
    }

    @DeleteMapping("/api/v1/messages/{messageId}/star")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unstarMessage(@PathVariable UUID messageId) {
        messageService.unstarMessage(messageId, CurrentUser.get().userId());
    }

    @GetMapping("/api/v1/messages/starred")
    public List<MessageResponse> listStarredMessages() {
        return messageService.listStarredMessages(CurrentUser.get().userId());
    }

    @PutMapping("/api/v1/chats/{chatId}/draft")
    public DraftResponse saveDraft(@PathVariable UUID chatId, @Valid @RequestBody DraftRequest request) {
        return messageService.saveDraft(chatId, CurrentUser.get().userId(), request);
    }

    @GetMapping("/api/v1/chats/{chatId}/draft")
    public DraftResponse getDraft(@PathVariable UUID chatId) {
        return messageService.getDraft(chatId, CurrentUser.get().userId());
    }

    @DeleteMapping("/api/v1/chats/{chatId}/draft")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDraft(@PathVariable UUID chatId) {
        messageService.deleteDraft(chatId, CurrentUser.get().userId());
    }
}
