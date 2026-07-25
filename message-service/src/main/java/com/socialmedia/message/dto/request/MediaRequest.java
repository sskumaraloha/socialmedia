package com.socialmedia.message.dto.request;

public record MediaRequest(String url, String mimeType, Long sizeBytes, Integer durationSeconds) {
}
