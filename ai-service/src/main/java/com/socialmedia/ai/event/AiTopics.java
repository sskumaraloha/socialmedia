package com.socialmedia.ai.event;

public final class AiTopics {

    // Consumed - message-service already publishes this for chat-service's benefit; this
    // service reacts to the same event to run spam/moderation checks on the message preview.
    public static final String MESSAGE_SENT = "message.sent.v1";

    // Published
    public static final String MESSAGE_MODERATED = "ai.message.moderated.v1";

    private AiTopics() {
    }
}
