package com.socialmedia.chat.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreatePrivateChatRequest(@NotNull UUID recipientUserId) {
}
