package com.socialmedia.media.dto.request;

import jakarta.validation.constraints.NotBlank;

public record InitiateUploadRequest(@NotBlank String originalFilename, @NotBlank String mimeType) {
}
