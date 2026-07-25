package com.socialmedia.ai.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TranslateRequest(@NotBlank String text, @NotBlank String targetLanguage) {
}
