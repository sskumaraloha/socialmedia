package com.socialmedia.user.controller;

import com.socialmedia.common.security.CurrentUser;
import com.socialmedia.user.dto.request.MuteUserRequest;
import com.socialmedia.user.dto.response.UserSummaryResponse;
import com.socialmedia.user.service.MuteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mutes")
@Tag(name = "Muting", description = "Mute/unmute other users (hides notifications, unlike blocking)")
public class MuteController {

    private final MuteService muteService;

    public MuteController(MuteService muteService) {
        this.muteService = muteService;
    }

    @GetMapping
    @Operation(summary = "List muted users")
    public List<UserSummaryResponse> list() {
        return muteService.listMuted(CurrentUser.get().userId());
    }

    @PostMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Mute a user, optionally until a given time")
    public void mute(@PathVariable UUID userId, @Valid @RequestBody(required = false) MuteUserRequest request) {
        muteService.mute(CurrentUser.get().userId(), userId, request == null ? new MuteUserRequest(null) : request);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Unmute a user")
    public void unmute(@PathVariable UUID userId) {
        muteService.unmute(CurrentUser.get().userId(), userId);
    }
}
