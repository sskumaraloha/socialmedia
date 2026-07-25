package com.socialmedia.notification.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.socialmedia.notification.domain.NotificationChannel;
import com.socialmedia.notification.domain.NotificationLog;
import com.socialmedia.notification.domain.NotificationStatus;
import com.socialmedia.notification.repository.NotificationLogRepository;
import com.socialmedia.notification.service.EmailSender;
import com.socialmedia.notification.service.PushSender;
import com.socialmedia.notification.service.SmsSender;
import com.socialmedia.notification.template.TemplateService;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationDispatchServiceTest {

    @Mock private EmailSender emailSender;
    @Mock private SmsSender smsSender;
    @Mock private PushSender pushSender;
    @Mock private NotificationLogRepository notificationLogRepository;

    private NotificationDispatchService service(
            EmailSender email, SmsSender sms, PushSender push, NotificationLogRepository logs) {
        return new NotificationDispatchService(new TemplateService(), email, sms, push, logs);
    }

    @Test
    void dispatchingEmailRendersTheTemplateAndLogsSuccess() {
        NotificationDispatchService dispatchService = service(emailSender, smsSender, pushSender, notificationLogRepository);
        UUID userId = UUID.randomUUID();

        dispatchService.dispatch(NotificationChannel.EMAIL, userId, "email-verification",
                Map.of("token", "ABC"), null, "alice@example.com", null);

        verify(emailSender).send(eq("alice@example.com"), eq("Verify your email address"), org.mockito.ArgumentMatchers.contains("ABC"));
        ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(logCaptor.capture());
        NotificationLog savedLog = logCaptor.getValue();
        org.assertj.core.api.Assertions.assertThat(savedLog.getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void dispatchingSmsCallsTheSmsSenderWithTheRenderedBody() {
        NotificationDispatchService dispatchService = service(emailSender, smsSender, pushSender, notificationLogRepository);
        UUID userId = UUID.randomUUID();

        dispatchService.dispatch(NotificationChannel.SMS, userId, "password-reset", Map.of("token", "999"), null,
                null, "+15551234567");

        verify(smsSender).send(eq("+15551234567"), org.mockito.ArgumentMatchers.contains("999"));
        verify(emailSender, never()).send(any(), any(), any());
    }

    @Test
    void dispatchingPushCallsThePushSenderWithTheUserId() {
        NotificationDispatchService dispatchService = service(emailSender, smsSender, pushSender, notificationLogRepository);
        UUID userId = UUID.randomUUID();

        dispatchService.dispatch(NotificationChannel.PUSH, userId, "new-message",
                Map.of("senderName", "Bob", "preview", "hey"), null, null, null);

        verify(pushSender).send(eq(userId), eq("New message from Bob"), org.mockito.ArgumentMatchers.contains("hey"));
    }

    @Test
    void aSenderFailureIsLoggedAndRethrownSoKafkaCanRetryAndDeadLetter() {
        NotificationDispatchService dispatchService = service(emailSender, smsSender, pushSender, notificationLogRepository);
        UUID userId = UUID.randomUUID();
        doThrow(new RuntimeException("SMTP timeout")).when(emailSender).send(any(), any(), any());

        assertThatThrownBy(() -> dispatchService.dispatch(NotificationChannel.EMAIL, userId, "email-verification",
                Map.of("token", "ABC"), null, "alice@example.com", null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("SMTP timeout");

        ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(logCaptor.capture());
        NotificationLog savedLog = logCaptor.getValue();
        org.assertj.core.api.Assertions.assertThat(savedLog.getStatus()).isEqualTo(NotificationStatus.FAILED);
        org.assertj.core.api.Assertions.assertThat(savedLog.getErrorMessage()).isEqualTo("SMTP timeout");
    }
}
