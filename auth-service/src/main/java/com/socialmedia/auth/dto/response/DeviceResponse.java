package com.socialmedia.auth.dto.response;

import com.socialmedia.auth.domain.DeviceType;
import java.time.Instant;
import java.util.UUID;

public record DeviceResponse(
        UUID id,
        String deviceId,
        String deviceName,
        DeviceType deviceType,
        Instant lastActiveAt,
        boolean trusted,
        boolean current
) {
}
