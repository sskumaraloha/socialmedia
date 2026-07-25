package com.socialmedia.user.controller;

import com.socialmedia.common.security.CurrentUser;
import com.socialmedia.user.dto.response.UserSummaryResponse;
import com.socialmedia.user.service.FollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/{userId}")
@Tag(name = "Followers / Following", description = "Asymmetric follow relationships")
public class FollowController {

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    @PostMapping("/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Follow a user")
    public void follow(@PathVariable UUID userId) {
        followService.follow(CurrentUser.get().userId(), userId);
    }

    @DeleteMapping("/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Unfollow a user")
    public void unfollow(@PathVariable UUID userId) {
        followService.unfollow(CurrentUser.get().userId(), userId);
    }

    @GetMapping("/followers")
    @Operation(summary = "List a user's followers")
    public Page<UserSummaryResponse> followers(@PathVariable UUID userId, Pageable pageable) {
        return followService.listFollowers(userId, pageable);
    }

    @GetMapping("/following")
    @Operation(summary = "List who a user is following")
    public Page<UserSummaryResponse> following(@PathVariable UUID userId, Pageable pageable) {
        return followService.listFollowing(userId, pageable);
    }
}
