package com.socialmedia.media.dto.response;

public record UploadedPartResponse(int partNumber, String eTag, long sizeBytes) {
}
