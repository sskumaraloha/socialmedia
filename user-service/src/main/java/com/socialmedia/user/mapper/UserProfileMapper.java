package com.socialmedia.user.mapper;

import com.socialmedia.user.domain.UserProfile;
import com.socialmedia.user.domain.Visibility;
import com.socialmedia.user.dto.response.PrivacySettingsResponse;
import com.socialmedia.user.dto.response.UserProfileResponse;
import com.socialmedia.user.dto.response.UserSummaryResponse;
import org.springframework.stereotype.Component;

@Component
public class UserProfileMapper {

    public UserSummaryResponse toSummary(UserProfile profile) {
        return new UserSummaryResponse(profile.getId(), profile.getUsername(), profile.getDisplayName(),
                profile.getAvatarUrl(), profile.isVerified());
    }

    public PrivacySettingsResponse toPrivacyResponse(UserProfile profile) {
        return new PrivacySettingsResponse(profile.getOnlineStatusVisibility(), profile.getLastSeenVisibility(),
                profile.getMessageableBy(), profile.getAddableToGroupsBy());
    }

    /**
     * @param isSelf true when the viewer is looking at their own profile - privacy settings never hide anything from yourself
     * @param viewerIsContact whether the viewer is in this profile owner's contacts
     */
    public UserProfileResponse toProfileResponse(UserProfile profile, boolean isSelf, boolean viewerIsContact,
            long followersCount, long followingCount, boolean followedByViewer, boolean followingViewer,
            boolean blockedByViewer, boolean mutedByViewer) {

        boolean canSeeOnline = isSelf || isVisible(profile.getOnlineStatusVisibility(), viewerIsContact);
        boolean canSeeLastSeen = isSelf || isVisible(profile.getLastSeenVisibility(), viewerIsContact);

        return new UserProfileResponse(
                profile.getId(),
                profile.getUsername(),
                profile.getDisplayName(),
                profile.getBio(),
                profile.getAvatarUrl(),
                profile.isVerified(),
                profile.getCustomStatus(),
                canSeeOnline ? isRecentlyOnline(profile) : null,
                canSeeLastSeen ? profile.getLastKnownOnlineAt() : null,
                followersCount,
                followingCount,
                followedByViewer,
                followingViewer,
                blockedByViewer,
                mutedByViewer
        );
    }

    private boolean isVisible(Visibility visibility, boolean viewerIsContact) {
        return switch (visibility) {
            case EVERYONE -> true;
            case CONTACTS -> viewerIsContact;
            case NOBODY -> false;
        };
    }

    private Boolean isRecentlyOnline(UserProfile profile) {
        if (profile.getLastKnownOnlineAt() == null) {
            return false;
        }
        return profile.getLastKnownOnlineAt().isAfter(java.time.Instant.now().minusSeconds(120));
    }
}
