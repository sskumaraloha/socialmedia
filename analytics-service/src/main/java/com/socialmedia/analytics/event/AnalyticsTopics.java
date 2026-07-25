package com.socialmedia.analytics.event;

public final class AnalyticsTopics {

    public static final String USER_REGISTERED = "auth.user.registered.v1";
    public static final String USER_LOGGED_IN = "auth.user.logged-in.v1";
    public static final String MESSAGE_SENT = "message.sent.v1";
    public static final String MEDIA_UPLOADED = "media.uploaded.v1";

    /** No producer exists yet anywhere in the platform (no payment-service); forward-declared so the
     * revenue dashboard is real and ready the moment one is built, following the same pattern
     * presence-service's presence.changed.v1 consumer used before presence-service existed. */
    public static final String PAYMENT_COMPLETED = "payment.completed.v1";

    private AnalyticsTopics() {
    }
}
