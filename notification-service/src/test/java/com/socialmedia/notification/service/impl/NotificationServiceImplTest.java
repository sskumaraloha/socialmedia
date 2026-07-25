package com.socialmedia.notification.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.socialmedia.notification.domain.DevicePlatform;
import com.socialmedia.notification.domain.DeviceToken;
import com.socialmedia.notification.domain.NotificationChannel;
import com.socialmedia.notification.domain.NotificationLog;
import com.socialmedia.notification.domain.NotificationStatus;
import com.socialmedia.notification.dto.request.RegisterDeviceTokenRequest;
import com.socialmedia.notification.dto.request.SendNotificationRequest;
import com.socialmedia.notification.event.NotificationEventPublisher;
import com.socialmedia.notification.event.outgoing.NotificationSendRequestEvent;
import com.socialmedia.notification.repository.DeviceTokenRepository;
import com.socialmedia.notification.repository.NotificationLogRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock private DeviceTokenRepository deviceTokenRepository;
    @Mock private NotificationLogRepository notificationLogRepository;
    @Mock private NotificationEventPublisher eventPublisher;

    private NotificationServiceImpl newService() {
        return new NotificationServiceImpl(deviceTokenRepository, notificationLogRepository, eventPublisher);
    }

    @Test
    void registeringADeviceTokenTwiceDoesNotCreateADuplicate() {
        UUID userId = UUID.randomUUID();
        when(deviceTokenRepository.findByUserIdAndToken(userId, "token-1")).thenReturn(
                Optional.of(new DeviceToken(userId, "token-1", DevicePlatform.ANDROID)));

        newService().registerDeviceToken(userId, new RegisterDeviceTokenRequest("token-1", DevicePlatform.ANDROID));

        verify(deviceTokenRepository, never()).save(any());
    }

    @Test
    void registeringANewDeviceTokenSavesIt() {
        UUID userId = UUID.randomUUID();
        when(deviceTokenRepository.findByUserIdAndToken(userId, "token-1")).thenReturn(Optional.empty());
        when(deviceTokenRepository.save(any(DeviceToken.class))).thenAnswer(inv -> inv.getArgument(0));

        newService().registerDeviceToken(userId, new RegisterDeviceTokenRequest("token-1", DevicePlatform.IOS));

        verify(deviceTokenRepository).save(any(DeviceToken.class));
    }

    @Test
    void requestSendPublishesAGenericNotificationEvent() {
        UUID userId = UUID.randomUUID();
        SendNotificationRequest request = new SendNotificationRequest(NotificationChannel.EMAIL, "email-verification",
                Map.of("token", "ABC"), "en", "alice@example.com", null);

        newService().requestSend(userId, request);

        ArgumentCaptor<NotificationSendRequestEvent> captor = ArgumentCaptor.forClass(NotificationSendRequestEvent.class);
        verify(eventPublisher).requestSend(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(userId);
        assertThat(captor.getValue().channel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(captor.getValue().recipientEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void listLogsMapsEntitiesToResponses() {
        UUID userId = UUID.randomUUID();
        NotificationLog log = new NotificationLog(userId, NotificationChannel.PUSH, "new-message", NotificationStatus.SENT, null);
        when(notificationLogRepository.findAllByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(log));

        var responses = newService().listLogs(userId);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).channel()).isEqualTo(NotificationChannel.PUSH);
        assertThat(responses.get(0).status()).isEqualTo(NotificationStatus.SENT);
    }
}
