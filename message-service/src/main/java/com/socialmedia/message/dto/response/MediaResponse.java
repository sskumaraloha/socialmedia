package com.socialmedia.message.dto.response;

public record MediaResponse(String url, String mimeType, Long sizeBytes, Integer durationSeconds) {
}
