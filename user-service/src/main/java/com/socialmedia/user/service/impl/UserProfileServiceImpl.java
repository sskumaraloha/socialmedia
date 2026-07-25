package com.socialmedia.user.service.impl;

import com.socialmedia.common.audit.AuditEventPublisher;
import com.socialmedia.common.exception.ResourceNotFoundException;
import com.socialmedia.user.domain.UserProfile;
import com.socialmedia.user.dto.request.UpdatePrivacyRequest;
import com.socialmedia.user.dto.request.UpdateProfileRequest;
import com.socialmedia.user.dto.request.UpdateUsernameRequest;
import com.socialmedia.user.dto.response.PrivacySettingsResponse;
import com.socialmedia.user.dto.response.UserProfileResponse;
import com.socialmedia.user.dto.response.UserSummaryResponse;
import com.socialmedia.user.event.UserEventPublisher;
import com.socialmedia.user.exception.UsernameAlreadyTakenException;
import com.socialmedia.user.mapper.UserProfileMapper;
import com.socialmedia.user.repository.BlockedUserRepository;
import com.socialmedia.user.repository.ContactRepository;
import com.socialmedia.user.repository.FollowRepository;
import com.socialmedia.user.repository.MutedUserRepository;
import com.socialmedia.user.repository.UserProfileRepository;
import com.socialmedia.user.service.UserProfileService;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileServiceImpl implements UserProfileService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserProfileRepository userProfileRepository;
    private final FollowRepository followRepository;
    private final ContactRepository contactRepository;
    private final BlockedUserRepository blockedUserRepository;
    private final MutedUserRepository mutedUserRepository;
    private final UserProfileMapper mapper;
    private final UserEventPublisher eventPublisher;
    private final AuditEventPublisher auditEventPublisher;

    public UserProfileServiceImpl(UserProfileRepository userProfileRepository, FollowRepository followRepository,
            ContactRepository contactRepository, BlockedUserRepository blockedUserRepository,
            MutedUserRepository mutedUserRepository, UserProfileMapper mapper, UserEventPublisher eventPublisher,
            AuditEventPublisher auditEventPublisher) {
        this.userProfileRepository = userProfileRepository;
        this.followRepository = followRepository;
        this.contactRepository = contactRepository;
        this.blockedUserRepository = blockedUserRepository;
        this.mutedUserRepository = mutedUserRepository;
        this.mapper = mapper;
        this.eventPublisher = eventPublisher;
        this.auditEventPublisher = auditEventPublisher;
    }

    @Override
    @Transactional
    public void createProfileForNewUser(UUID userId, String email) {
        if (userProfileRepository.existsById(userId)) {
            return; // already handled - Kafka delivery is at-least-once
        }
        String username = generateUniqueUsername(email);
        UserProfile profile = new UserProfile(userId, username);
        userProfileRepository.save(profile);
        eventPublisher.publishProfileCreated(profile);
    }

    private String generateUniqueUsername(String email) {
        String base = email.substring(0, email.indexOf('@'))
                .toLowerCase()
                .replaceAll("[^a-z0-9_.]", "");
        if (base.length() < 3) {
            base = base + "user";
        }
        base = base.substring(0, Math.min(base.length(), 24));

        String candidate = base;
        while (userProfileRepository.existsByUsernameIgnoreCase(candidate)) {
            candidate = base + (1000 + RANDOM.nextInt(9000));
        }
        return candidate;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID profileOwnerId, UUID viewerId) {
        UserProfile profile = require(profileOwnerId);
        boolean isSelf = profileOwnerId.equals(viewerId);

        boolean viewerIsContact = !isSelf && viewerId != null
                && contactRepository.existsByOwnerIdAndContactUserId(profileOwnerId, viewerId);
        long followersCount = followRepository.countByFollowingId(profileOwnerId);
        long followingCount = followRepository.countByFollowerId(profileOwnerId);
        boolean followedByViewer = viewerId != null
                && followRepository.existsByFollowerIdAndFollowingId(viewerId, profileOwnerId);
        boolean followingViewer = viewerId != null
                && followRepository.existsByFollowerIdAndFollowingId(profileOwnerId, viewerId);
        boolean blockedByViewer = viewerId != null
                && blockedUserRepository.existsByBlockerIdAndBlockedId(viewerId, profileOwnerId);
        boolean mutedByViewer = viewerId != null
                && mutedUserRepository.findByMuterIdAndMutedId(viewerId, profileOwnerId).isPresent();

        return mapper.toProfileResponse(profile, isSelf, viewerIsContact, followersCount, followingCount,
                followedByViewer, followingViewer, blockedByViewer, mutedByViewer);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        UserProfile profile = require(userId);
        if (request.displayName() != null) {
            profile.setDisplayName(request.displayName());
        }
        if (request.bio() != null) {
            profile.setBio(request.bio());
        }
        if (request.avatarUrl() != null) {
            profile.setAvatarUrl(request.avatarUrl());
        }
        if (request.customStatus() != null) {
            profile.setCustomStatus(request.customStatus());
        }
        userProfileRepository.save(profile);
        eventPublisher.publishProfileUpdated(profile);
        return getProfile(userId, userId);
    }

    @Override
    @Transactional
    public UserProfileResponse updateUsername(UUID userId, UpdateUsernameRequest request) {
        UserProfile profile = require(userId);
        if (!profile.getUsername().equalsIgnoreCase(request.username())
                && userProfileRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new UsernameAlreadyTakenException(request.username());
        }
        profile.setUsername(request.username());
        userProfileRepository.save(profile);
        eventPublisher.publishProfileUpdated(profile);
        return getProfile(userId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public PrivacySettingsResponse getPrivacySettings(UUID userId) {
        return mapper.toPrivacyResponse(require(userId));
    }

    @Override
    @Transactional
    public PrivacySettingsResponse updatePrivacySettings(UUID userId, UpdatePrivacyRequest request) {
        UserProfile profile = require(userId);
        profile.setOnlineStatusVisibility(request.onlineStatusVisibility());
        profile.setLastSeenVisibility(request.lastSeenVisibility());
        profile.setMessageableBy(request.messageableBy());
        profile.setAddableToGroupsBy(request.addableToGroupsBy());
        userProfileRepository.save(profile);
        auditEventPublisher.publish(userId, "PRIVACY_SETTINGS_UPDATED", "USER", userId.toString(), Map.of(
                "onlineStatusVisibility", request.onlineStatusVisibility().name(),
                "lastSeenVisibility", request.lastSeenVisibility().name(),
                "messageableBy", request.messageableBy().name(),
                "addableToGroupsBy", request.addableToGroupsBy().name()));
        return mapper.toPrivacyResponse(profile);
    }

    @Override
    @Transactional
    public void updateLastKnownOnline(UUID userId, boolean online, Instant lastSeenAt) {
        userProfileRepository.findById(userId).ifPresent(profile -> {
            profile.setLastKnownOnlineAt(online ? Instant.now() : lastSeenAt);
            userProfileRepository.save(profile);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserSummaryResponse> searchUsers(String query, Pageable pageable) {
        return userProfileRepository.search(query, pageable).map(mapper::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummaryResponse> getSuggestions(UUID userId, int limit) {
        // Placeholder heuristic: verified accounts the user doesn't already follow, most
        // recently created first. A real recommendation engine (mutual-follow graph,
        // interest overlap, etc.) is a natural next-phase enhancement once there's
        // enough interaction data to rank on.
        Pageable pageable = PageRequest.of(0, limit + 1);
        return userProfileRepository.findAll(pageable).stream()
                .filter(p -> !p.getId().equals(userId))
                .filter(p -> !followRepository.existsByFollowerIdAndFollowingId(userId, p.getId()))
                .limit(limit)
                .map(mapper::toSummary)
                .collect(Collectors.toList());
    }

    private UserProfile require(UUID userId) {
        return userProfileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("UserProfile", userId));
    }
}
