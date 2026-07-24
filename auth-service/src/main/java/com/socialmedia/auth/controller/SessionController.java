package com.socialmedia.auth.controller;

import com.socialmedia.auth.dto.response.MessageResponse;
import com.socialmedia.auth.dto.response.SessionResponse;
import com.socialmedia.auth.security.CurrentUserProvider;
import com.socialmedia.auth.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/sessions")
@Tag(name = "Session Management", description = "List and revoke active login sessions")
public class SessionController {

    private final SessionService sessionService;
    private final CurrentUserProvider currentUserProvider;

    public SessionController(SessionService sessionService, CurrentUserProvider currentUserProvider) {
        this.sessionService = sessionService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    @Operation(summary = "List active sessions for the current account")
    public List<SessionResponse> list() {
        return sessionService.listForUser(currentUserProvider.get(), currentUserProvider.jti().orElse(null));
    }

    @DeleteMapping("/{sessionId}")
    @Operation(summary = "Revoke a specific session")
    public MessageResponse revoke(@PathVariable UUID sessionId) {
        sessionService.revoke(currentUserProvider.get(), sessionId);
        return new MessageResponse("Session revoked");
    }

    @PostMapping("/revoke-others")
    @Operation(summary = "Revoke every session except the one making this request")
    public MessageResponse revokeOthers() {
        sessionService.revokeAllExceptCurrent(currentUserProvider.get(), currentUserProvider.jti().orElse(null));
        return new MessageResponse("Other sessions revoked");
    }
}
