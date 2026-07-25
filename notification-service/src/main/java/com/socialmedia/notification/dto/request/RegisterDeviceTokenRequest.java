package com.socialmedia.notification.dto.request;

import com.socialmedia.notification.domain.DevicePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterDeviceTokenRequest(@NotBlank String token, @NotNull DevicePlatform platform) {
}
