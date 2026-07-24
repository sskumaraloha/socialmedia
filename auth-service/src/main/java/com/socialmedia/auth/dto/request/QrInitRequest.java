package com.socialmedia.auth.dto.request;

import com.socialmedia.auth.domain.DeviceType;
import jakarta.validation.constraints.NotBlank;

public record QrInitRequest(
        @NotBlank String deviceId,
        String deviceName,
        DeviceType deviceType
) {
}
