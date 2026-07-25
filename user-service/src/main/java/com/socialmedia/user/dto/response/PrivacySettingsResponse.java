package com.socialmedia.user.dto.response;

import com.socialmedia.user.domain.Visibility;

public record PrivacySettingsResponse(
        Visibility onlineStatusVisibility,
        Visibility lastSeenVisibility,
        Visibility messageableBy,
        Visibility addableToGroupsBy
) {
}
