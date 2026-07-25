package com.socialmedia.notification.event;

public final class NotificationTopics {

    // Consumed - auth-service already publishes these (see docs/ARCHITECTURE.md: "actual
    // sending is notification-service's job once built").
    public static final String EMAIL_VERIFICATION_REQUESTED = "auth.email-verification-requested.v1";
    public static final String PASSWORD_RESET_REQUESTED = "auth.password.reset-requested.v1";

    // A generic self-topic any service (or this service's own REST API) can publish to,
    // so notification-service doesn't need a bespoke consumer for every future event type.
    public static final String NOTIFICATION_SEND_REQUESTED = "notification.send.v1";

    private NotificationTopics() {
    }
}
