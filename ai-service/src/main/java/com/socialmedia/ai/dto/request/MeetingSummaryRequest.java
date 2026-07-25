package com.socialmedia.ai.dto.request;

import jakarta.validation.constraints.NotBlank;

public record MeetingSummaryRequest(@NotBlank String transcript) {
}
