package com.socialmedia.notification.service;

import com.socialmedia.notification.dto.request.RegisterDeviceTokenRequest;
import com.socialmedia.notification.dto.request.SendNotificationRequest;
import com.socialmedia.notification.dto.response.NotificationLogResponse;
import java.util.List;
import java.util.UUID;

public interface NotificationService {

    void registerDeviceToken(UUID userId, RegisterDeviceTokenRequest request);

    void unregisterDeviceToken(UUID userId, String token);

    void requestSend(UUID userId, SendNotificationRequest request);

    List<NotificationLogResponse> listLogs(UUID userId);
}
