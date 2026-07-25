package com.socialmedia.user.controller;

import com.socialmedia.common.security.CurrentUser;
import com.socialmedia.user.dto.request.UpdatePrivacyRequest;
import com.socialmedia.user.dto.request.UpdateProfileRequest;
import com.socialmedia.user.dto.request.UpdateUsernameRequest;
import com.socialmedia.user.dto.response.PrivacySettingsResponse;
import com.socialmedia.user.dto.response.UserProfileResponse;
import com.socialmedia.user.dto.response.UserSummaryResponse;
import com.socialmedia.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Profiles", description = "Profile, privacy, search, and suggestions")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/me")
    @Operation(summary = "Current user's own profile")
    public UserProfileResponse me() {
        UUID userId = CurrentUser.get().userId();
        return userProfileService.getProfile(userId, userId);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "View another user's profile (privacy-filtered for the viewer)")
    public UserProfileResponse getProfile(@PathVariable UUID userId) {
        return userProfileService.getProfile(userId, CurrentUser.get().userId());
    }

    @PatchMapping("/me")
    @Operation(summary = "Update display name, bio, avatar, and/or custom status")
    public UserProfileResponse updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return userProfileService.updateProfile(CurrentUser.get().userId(), request);
    }

    @PatchMapping("/me/username")
    @Operation(summary = "Change username")
    public UserProfileResponse updateUsername(@Valid @RequestBody UpdateUsernameRequest request) {
        return userProfileService.updateUsername(CurrentUser.get().userId(), request);
    }

    @GetMapping("/me/privacy")
    @Operation(summary = "Current privacy settings")
    public PrivacySettingsResponse getPrivacySettings() {
        return userProfileService.getPrivacySettings(CurrentUser.get().userId());
    }

    @PatchMapping("/me/privacy")
    @Operation(summary = "Update privacy settings")
    public PrivacySettingsResponse updatePrivacySettings(@Valid @RequestBody UpdatePrivacyRequest request) {
        return userProfileService.updatePrivacySettings(CurrentUser.get().userId(), request);
    }

    @GetMapping("/search")
    @Operation(summary = "Search users by username or display name")
    public Page<UserSummaryResponse> search(@RequestParam("q") String query, Pageable pageable) {
        return userProfileService.searchUsers(query, pageable);
    }

    @GetMapping("/suggestions")
    @Operation(summary = "People-you-may-know style suggestions")
    public List<UserSummaryResponse> suggestions(@RequestParam(defaultValue = "20") int limit) {
        return userProfileService.getSuggestions(CurrentUser.get().userId(), limit);
    }
}
