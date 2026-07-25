package com.socialmedia.ai.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SummarizeRequest(@NotBlank String text) {
}
