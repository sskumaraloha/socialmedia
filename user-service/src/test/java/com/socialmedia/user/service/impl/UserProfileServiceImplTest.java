package com.socialmedia.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.socialmedia.user.domain.UserProfile;
import com.socialmedia.user.dto.request.UpdateUsernameRequest;
import com.socialmedia.user.dto.response.UserProfileResponse;
import com.socialmedia.user.event.UserEventPublisher;
import com.socialmedia.user.exception.UsernameAlreadyTakenException;
import com.socialmedia.user.mapper.UserProfileMapper;
import com.socialmedia.user.repository.BlockedUserRepository;
import com.socialmedia.user.repository.ContactRepository;
import com.socialmedia.user.repository.FollowRepository;
import com.socialmedia.user.repository.MutedUserRepository;
import com.socialmedia.user.repository.UserProfileRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {

    @Mock private UserProfileRepository userProfileRepository;
    @Mock private FollowRepository followRepository;
    @Mock private ContactRepository contactRepository;
    @Mock private BlockedUserRepository blockedUserRepository;
    @Mock private MutedUserRepository mutedUserRepository;
    @Mock private UserEventPublisher eventPublisher;

    private UserProfileServiceImpl service;

    @BeforeEach
    void setUp() {
        UserProfileMapper mapper = new UserProfileMapper();
        service = new UserProfileServiceImpl(userProfileRepository, followRepository, contactRepository,
                blockedUserRepository, mutedUserRepository, mapper, eventPublisher);
        lenient().when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createProfileForNewUserDerivesUsernameFromEmailLocalPart() {
        UUID userId = UUID.randomUUID();
        when(userProfileRepository.existsById(userId)).thenReturn(false);
        when(userProfileRepository.existsByUsernameIgnoreCase("alice")).thenReturn(false);

        service.createProfileForNewUser(userId, "alice@example.com");

        verify(userProfileRepository).save(argThatUsernameEquals("alice"));
        verify(eventPublisher).publishProfileCreated(any(UserProfile.class));
    }

    @Test
    void createProfileForNewUserAppendsASuffixWhenTheDerivedUsernameIsTaken() {
        UUID userId = UUID.randomUUID();
        when(userProfileRepository.existsById(userId)).thenReturn(false);
        when(userProfileRepository.existsByUsernameIgnoreCase(anyString()))
                .thenAnswer(inv -> inv.getArgument(0, String.class).equals("bob"));

        service.createProfileForNewUser(userId, "bob@example.com");

        verify(userProfileRepository).save(argThat(profile -> profile.getUsername().startsWith("bob")
                && !profile.getUsername().equals("bob")));
    }

    @Test
    void createProfileForNewUserIsIdempotent() {
        UUID userId = UUID.randomUUID();
        when(userProfileRepository.existsById(userId)).thenReturn(true);

        service.createProfileForNewUser(userId, "alice@example.com");

        verify(userProfileRepository, never()).save(any());
        verify(eventPublisher, never()).publishProfileCreated(any());
    }

    @Test
    void updateUsernameRejectsAnAlreadyTakenUsername() {
        UUID userId = UUID.randomUUID();
        UserProfile profile = new UserProfile(userId, "alice");
        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(userProfileRepository.existsByUsernameIgnoreCase("bob")).thenReturn(true);

        assertThatThrownBy(() -> service.updateUsername(userId, new UpdateUsernameRequest("bob")))
                .isInstanceOf(UsernameAlreadyTakenException.class);
    }

    @Test
    void getProfileHidesLastSeenWhenVisibilityIsNobodyAndViewerIsNotSelf() {
        UUID ownerId = UUID.randomUUID();
        UUID viewerId = UUID.randomUUID();
        UserProfile profile = new UserProfile(ownerId, "alice");
        profile.setLastSeenVisibility(com.socialmedia.user.domain.Visibility.NOBODY);
        profile.setOnlineStatusVisibility(com.socialmedia.user.domain.Visibility.NOBODY);
        profile.setLastKnownOnlineAt(java.time.Instant.now());

        when(userProfileRepository.findById(ownerId)).thenReturn(Optional.of(profile));
        when(contactRepository.existsByOwnerIdAndContactUserId(ownerId, viewerId)).thenReturn(false);
        when(followRepository.countByFollowingId(ownerId)).thenReturn(0L);
        when(followRepository.countByFollowerId(ownerId)).thenReturn(0L);

        UserProfileResponse response = service.getProfile(ownerId, viewerId);

        assertThat(response.lastSeenAt()).isNull();
        assertThat(response.online()).isNull();
    }

    @Test
    void getProfileAlwaysShowsEverythingToTheOwnerThemselves() {
        UUID ownerId = UUID.randomUUID();
        UserProfile profile = new UserProfile(ownerId, "alice");
        profile.setLastSeenVisibility(com.socialmedia.user.domain.Visibility.NOBODY);
        profile.setLastKnownOnlineAt(java.time.Instant.now());

        when(userProfileRepository.findById(ownerId)).thenReturn(Optional.of(profile));
        when(followRepository.countByFollowingId(ownerId)).thenReturn(0L);
        when(followRepository.countByFollowerId(ownerId)).thenReturn(0L);

        UserProfileResponse response = service.getProfile(ownerId, ownerId);

        assertThat(response.lastSeenAt()).isNotNull();
    }

    private static UserProfile argThatUsernameEquals(String username) {
        return org.mockito.ArgumentMatchers.argThat(profile -> profile.getUsername().equals(username));
    }

    private static UserProfile argThat(java.util.function.Predicate<UserProfile> predicate) {
        return org.mockito.ArgumentMatchers.argThat(predicate::test);
    }
}
