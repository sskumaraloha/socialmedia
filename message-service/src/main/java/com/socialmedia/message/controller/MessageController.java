package com.socialmedia.message.controller;

import com.socialmedia.message.dto.request.DraftRequest;
import com.socialmedia.message.dto.request.EditMessageRequest;
import com.socialmedia.message.dto.request.ForwardMessageRequest;
import com.socialmedia.message.dto.request.ReactionRequest;
import com.socialmedia.message.dto.request.SendMessageRequest;
import com.socialmedia.message.dto.response.DraftResponse;
import com.socialmedia.message.dto.response.MessageResponse;
import com.socialmedia.message.service.EncryptedPayloadValidator;
import com.socialmedia.message.service.MessageService;
import com.socialmedia.common.security.AuthenticatedPrincipal;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read/write paths pass the caller's deviceId (from the JWT's deviceId claim, already populated
 * platform-wide by common-library's JwtAuthenticationFilter) down to the service, because under
 * per-device end-to-end encryption a message's ciphertext is addressed to a device, not a user.
 */
@RestController
public class MessageController {

    private final MessageService messageService;
    private final EncryptedPayloadValidator payloadValidator;

    public MessageController(MessageService messageService, EncryptedPayloadValidator payloadValidator) {
        this.messageService = messageService;
        this.payloadValidator = payloadValidator;
    }

    private static AuthenticatedPrincipal caller() {
        return CurrentUser.get();
    }

    @PostMapping("/api/v1/chats/{chatId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse sendMessage(@PathVariable UUID chatId, @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody SendMessageRequest request) {
        AuthenticatedPrincipal caller = caller();
        return messageService.sendMessage(chatId, caller.userId(), caller.deviceId(), authorization, request);
    }

    @GetMapping("/api/v1/chats/{chatId}/messages")
    public List<MessageResponse> listMessages(@PathVariable UUID chatId, @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) Instant before, @RequestParam(defaultValue = "50") int limit) {
        AuthenticatedPrincipal caller = caller();
        return messageService.listMessages(chatId, caller.userId(), caller.deviceId(), authorization, before, limit);
    }

    @GetMapping("/api/v1/messages/{messageId}")
    public MessageResponse getMessage(@PathVariable UUID messageId, @RequestHeader("Authorization") String authorization) {
        AuthenticatedPrincipal caller = caller();
        return messageService.getMessage(messageId, caller.userId(), caller.deviceId(), authorization);
    }

    @PatchMapping("/api/v1/messages/{messageId}")
    public MessageResponse editMessage(@PathVariable UUID messageId, @Valid @RequestBody EditMessageRequest request) {
        AuthenticatedPrincipal caller = caller();
        return messageService.editMessage(messageId, caller.userId(), caller.deviceId(), request);
    }

    @DeleteMapping("/api/v1/messages/{messageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMessage(@PathVariable UUID messageId, @RequestParam(defaultValue = "false") boolean forEveryone) {
        messageService.deleteMessage(messageId, caller().userId(), forEveryone);
    }

    @PostMapping("/api/v1/messages/{messageId}/forward")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse forwardMessage(@PathVariable UUID messageId, @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody ForwardMessageRequest request) {
        AuthenticatedPrincipal caller = caller();
        return messageService.forwardMessage(messageId, caller.userId(), caller.deviceId(), authorization,
                request.targetChatId(), payloadValidator.validateAndConvert(request.envelopes()));
    }

    @PutMapping("/api/v1/messages/{messageId}/reactions")
    public MessageResponse reactToMessage(@PathVariable UUID messageId, @Valid @RequestBody ReactionRequest request) {
        AuthenticatedPrincipal caller = caller();
        return messageService.reactToMessage(messageId, caller.userId(), caller.deviceId(), request.emoji());
    }

    @DeleteMapping("/api/v1/messages/{messageId}/reactions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeReaction(@PathVariable UUID messageId) {
        messageService.removeReaction(messageId, caller().userId());
    }

    @PostMapping("/api/v1/messages/{messageId}/delivered")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markDelivered(@PathVariable UUID messageId) {
        messageService.markDelivered(messageId, caller().userId());
    }

    @PostMapping("/api/v1/messages/{messageId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable UUID messageId) {
        messageService.markRead(messageId, caller().userId());
    }

    @PutMapping("/api/v1/messages/{messageId}/star")
    public MessageResponse starMessage(@PathVariable UUID messageId) {
        AuthenticatedPrincipal caller = caller();
        return messageService.starMessage(messageId, caller.userId(), caller.deviceId());
    }

    @DeleteMapping("/api/v1/messages/{messageId}/star")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unstarMessage(@PathVariable UUID messageId) {
        messageService.unstarMessage(messageId, caller().userId());
    }

    @GetMapping("/api/v1/messages/starred")
    public List<MessageResponse> listStarredMessages() {
        AuthenticatedPrincipal caller = caller();
        return messageService.listStarredMessages(caller.userId(), caller.deviceId());
    }

    @PutMapping("/api/v1/chats/{chatId}/draft")
    public DraftResponse saveDraft(@PathVariable UUID chatId, @Valid @RequestBody DraftRequest request) {
        return messageService.saveDraft(chatId, caller().userId(), request);
    }

    @GetMapping("/api/v1/chats/{chatId}/draft")
    public DraftResponse getDraft(@PathVariable UUID chatId) {
        return messageService.getDraft(chatId, caller().userId());
    }

    @DeleteMapping("/api/v1/chats/{chatId}/draft")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDraft(@PathVariable UUID chatId) {
        messageService.deleteDraft(chatId, caller().userId());
    }
}
