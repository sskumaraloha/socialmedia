package com.socialmedia.chat.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.Set;
import java.util.UUID;

public record AddMembersRequest(@NotEmpty Set<UUID> userIds) {
}
