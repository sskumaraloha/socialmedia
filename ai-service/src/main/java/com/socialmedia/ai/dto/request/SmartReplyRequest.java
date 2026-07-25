package com.socialmedia.ai.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SmartReplyRequest(@NotBlank String conversationContext, Integer count) {
}
