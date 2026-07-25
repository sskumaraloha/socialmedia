package com.socialmedia.notification.service.impl;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.socialmedia.notification.domain.DeviceToken;
import com.socialmedia.notification.exception.PushUnavailableException;
import com.socialmedia.notification.repository.DeviceTokenRepository;
import com.socialmedia.notification.service.PushSender;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FcmPushSender implements PushSender {

    private static final Logger log = LoggerFactory.getLogger(FcmPushSender.class);

    private final DeviceTokenRepository deviceTokenRepository;
    private final FirebasePushGateway gateway;

    public FcmPushSender(DeviceTokenRepository deviceTokenRepository, FirebasePushGateway gateway) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.gateway = gateway;
    }

    @Override
    public void send(UUID userId, String title, String body) {
        if (!gateway.isAvailable()) {
            throw new PushUnavailableException("Firebase is not configured in this deployment");
        }

        List<DeviceToken> tokens = deviceTokenRepository.findAllByUserId(userId);
        if (tokens.isEmpty()) {
            log.debug("No device tokens registered for user {} - nothing to push", userId);
            return;
        }

        int failures = 0;
        for (DeviceToken deviceToken : tokens) {
            try {
                gateway.send(deviceToken.getToken(), title, body);
            } catch (FirebaseMessagingException e) {
                failures++;
                log.warn("Failed to deliver push to a device for user {}: {}", userId, e.getMessage());
            }
        }

        if (failures == tokens.size()) {
            throw new PushUnavailableException("Push delivery failed for every registered device of user " + userId);
        }
    }
}
