package com.socialmedia.chat.dto.request;

import com.socialmedia.chat.domain.ChatMemberRole;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(@NotNull ChatMemberRole role) {
}
