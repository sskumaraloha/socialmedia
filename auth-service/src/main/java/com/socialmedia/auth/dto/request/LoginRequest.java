package com.socialmedia.auth.dto.request;

import com.socialmedia.auth.domain.DeviceType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password,
        @NotBlank String deviceId,
        String deviceName,
        DeviceType deviceType
) {
}
