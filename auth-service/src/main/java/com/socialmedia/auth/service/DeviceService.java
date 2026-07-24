package com.socialmedia.auth.service;

import com.socialmedia.auth.domain.Device;
import com.socialmedia.auth.domain.DeviceType;
import com.socialmedia.auth.domain.User;
import com.socialmedia.auth.dto.response.DeviceResponse;
import java.util.List;

public interface DeviceService {

    /** Creates the device record on first login, or touches lastActiveAt if it already exists. */
    Device upsert(User user, String deviceId, String deviceName, DeviceType deviceType);

    List<DeviceResponse> listForUser(User user, String currentDeviceId);

    void revoke(User user, String deviceId);
}
