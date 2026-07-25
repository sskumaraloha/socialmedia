package com.socialmedia.ai.dto.response;

public record SpamCheckResponse(boolean spam, double confidence, String reason) {
}
