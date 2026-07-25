package com.socialmedia.ai.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Shared shape for spam-check / moderation / sentiment - each just needs a piece of text. */
public record TextRequest(@NotBlank String text) {
}
