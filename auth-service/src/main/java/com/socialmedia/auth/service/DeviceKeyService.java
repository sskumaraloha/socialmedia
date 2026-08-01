package com.socialmedia.auth.service;

import com.socialmedia.auth.domain.User;
import com.socialmedia.auth.dto.request.UploadKeyBundleRequest;
import com.socialmedia.auth.dto.response.KeyBundleResponse;
import com.socialmedia.auth.dto.response.PreKeyCountResponse;

public interface DeviceKeyService {

    void uploadKeyBundle(User user, String deviceId, UploadKeyBundleRequest request);

    KeyBundleResponse getKeyBundle(String deviceId);

    PreKeyCountResponse getPreKeyCount(User user, String deviceId);
}
