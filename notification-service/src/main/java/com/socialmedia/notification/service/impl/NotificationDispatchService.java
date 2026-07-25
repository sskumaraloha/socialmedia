package com.socialmedia.notification.service.impl;

import com.socialmedia.notification.domain.NotificationChannel;
import com.socialmedia.notification.domain.NotificationLog;
import com.socialmedia.notification.domain.NotificationStatus;
import com.socialmedia.notification.repository.NotificationLogRepository;
import com.socialmedia.notification.service.EmailSender;
import com.socialmedia.notification.service.PushSender;
import com.socialmedia.notification.service.SmsSender;
import com.socialmedia.notification.template.TemplateResult;
import com.socialmedia.notification.template.TemplateService;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * The single place every channel funnels through: render the localized template, hand
 * off to the channel-specific sender, and log the outcome. Failures are rethrown after
 * logging rather than swallowed - every caller of dispatch() is a @KafkaListener, and
 * this service's KafkaConsumerConfig wires a retry-then-dead-letter error handler on the
 * container factory, so a rethrown exception here is what actually drives retry/DLQ.
 */
@Service
public class NotificationDispatchService {

    private final TemplateService templateService;
    private final EmailSender emailSender;
    private final SmsSender smsSender;
    private final PushSender pushSender;
    private final NotificationLogRepository notificationLogRepository;

    public NotificationDispatchService(TemplateService templateService, EmailSender emailSender, SmsSender smsSender,
            PushSender pushSender, NotificationLogRepository notificationLogRepository) {
        this.templateService = templateService;
        this.emailSender = emailSender;
        this.smsSender = smsSender;
        this.pushSender = pushSender;
        this.notificationLogRepository = notificationLogRepository;
    }

    public void dispatch(NotificationChannel channel, UUID userId, String templateKey, Map<String, String> params,
            String locale, String recipientEmail, String recipientPhone) {
        TemplateResult rendered = templateService.render(templateKey, locale, params);
        try {
            switch (channel) {
                case EMAIL -> emailSender.send(recipientEmail, rendered.subject(), rendered.body());
                case SMS -> smsSender.send(recipientPhone, rendered.body());
                case PUSH -> pushSender.send(userId, rendered.subject(), rendered.body());
            }
            notificationLogRepository.save(new NotificationLog(userId, channel, templateKey, NotificationStatus.SENT, null));
        } catch (Exception e) {
            notificationLogRepository.save(new NotificationLog(userId, channel, templateKey, NotificationStatus.FAILED, e.getMessage()));
            throw e;
        }
    }
}
