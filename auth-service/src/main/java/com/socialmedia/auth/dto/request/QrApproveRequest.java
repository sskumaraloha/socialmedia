package com.socialmedia.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record QrApproveRequest(@NotBlank String qrToken) {
}
