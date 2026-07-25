package com.socialmedia.notification.domain;

import com.socialmedia.common.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "notification_logs")
public class NotificationLog extends BaseEntity {

    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    @Column(nullable = false, length = 100)
    private String templateKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status;

    @Column(length = 1000)
    private String errorMessage;

    protected NotificationLog() {
    }

    public NotificationLog(UUID userId, NotificationChannel channel, String templateKey, NotificationStatus status,
            String errorMessage) {
        this.userId = userId;
        this.channel = channel;
        this.templateKey = templateKey;
        this.status = status;
        this.errorMessage = errorMessage;
    }

    public UUID getUserId() {
        return userId;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public String getTemplateKey() {
        return templateKey;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
