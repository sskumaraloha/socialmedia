package com.socialmedia.media.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CompletedPartRequest(@Positive int partNumber, @NotBlank String eTag) {
}
