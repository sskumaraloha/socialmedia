package com.socialmedia.message.dto.request;

import jakarta.validation.constraints.NotNull;

public record DraftRequest(@NotNull String content) {
}
