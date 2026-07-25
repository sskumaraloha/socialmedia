package com.socialmedia.message.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ForwardMessageRequest(@NotNull UUID targetChatId) {
}
