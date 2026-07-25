package com.socialmedia.notification.service.impl;

import com.socialmedia.notification.domain.DeviceToken;
import com.socialmedia.notification.dto.request.RegisterDeviceTokenRequest;
import com.socialmedia.notification.dto.request.SendNotificationRequest;
import com.socialmedia.notification.dto.response.NotificationLogResponse;
import com.socialmedia.notification.event.NotificationEventPublisher;
import com.socialmedia.notification.event.outgoing.NotificationSendRequestEvent;
import com.socialmedia.notification.repository.DeviceTokenRepository;
import com.socialmedia.notification.repository.NotificationLogRepository;
import com.socialmedia.notification.service.NotificationService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final NotificationEventPublisher eventPublisher;

    public NotificationServiceImpl(DeviceTokenRepository deviceTokenRepository,
            NotificationLogRepository notificationLogRepository, NotificationEventPublisher eventPublisher) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.notificationLogRepository = notificationLogRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void registerDeviceToken(UUID userId, RegisterDeviceTokenRequest request) {
        deviceTokenRepository.findByUserIdAndToken(userId, request.token())
                .orElseGet(() -> deviceTokenRepository.save(new DeviceToken(userId, request.token(), request.platform())));
    }

    @Override
    public void unregisterDeviceToken(UUID userId, String token) {
        deviceTokenRepository.deleteByUserIdAndToken(userId, token);
    }

    @Override
    public void requestSend(UUID userId, SendNotificationRequest request) {
        eventPublisher.requestSend(new NotificationSendRequestEvent(userId, request.channel(), request.templateKey(),
                request.templateParams(), request.locale(), request.recipientEmail(), request.recipientPhone()));
    }

    @Override
    public List<NotificationLogResponse> listLogs(UUID userId) {
        return notificationLogRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(log -> new NotificationLogResponse(log.getId(), log.getChannel(), log.getTemplateKey(),
                        log.getStatus(), log.getErrorMessage(), log.getCreatedAt()))
                .toList();
    }
}
