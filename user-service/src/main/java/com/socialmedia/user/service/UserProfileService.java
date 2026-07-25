package com.socialmedia.user.service;

import com.socialmedia.user.dto.request.UpdatePrivacyRequest;
import com.socialmedia.user.dto.request.UpdateProfileRequest;
import com.socialmedia.user.dto.request.UpdateUsernameRequest;
import com.socialmedia.user.dto.response.PrivacySettingsResponse;
import com.socialmedia.user.dto.response.UserProfileResponse;
import com.socialmedia.user.dto.response.UserSummaryResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserProfileService {

    /** Reacts to auth-service's UserRegisteredEvent - idempotent (safe to call twice for the same userId). */
    void createProfileForNewUser(UUID userId, String email);

    UserProfileResponse getProfile(UUID profileOwnerId, UUID viewerId);

    UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request);

    UserProfileResponse updateUsername(UUID userId, UpdateUsernameRequest request);

    PrivacySettingsResponse getPrivacySettings(UUID userId);

    PrivacySettingsResponse updatePrivacySettings(UUID userId, UpdatePrivacyRequest request);

    void updateLastKnownOnline(UUID userId, boolean online, Instant lastSeenAt);

    Page<UserSummaryResponse> searchUsers(String query, Pageable pageable);

    List<UserSummaryResponse> getSuggestions(UUID userId, int limit);
}
