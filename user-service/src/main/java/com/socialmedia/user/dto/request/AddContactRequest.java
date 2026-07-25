package com.socialmedia.user.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddContactRequest(
        @NotNull UUID contactUserId,
        String nickname
) {
}
