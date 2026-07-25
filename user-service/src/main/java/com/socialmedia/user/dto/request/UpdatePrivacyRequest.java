package com.socialmedia.user.dto.request;

import com.socialmedia.user.domain.Visibility;
import jakarta.validation.constraints.NotNull;

public record UpdatePrivacyRequest(
        @NotNull Visibility onlineStatusVisibility,
        @NotNull Visibility lastSeenVisibility,
        @NotNull Visibility messageableBy,
        @NotNull Visibility addableToGroupsBy
) {
}
