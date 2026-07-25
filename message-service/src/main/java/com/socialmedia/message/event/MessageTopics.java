package com.socialmedia.message.event;

public final class MessageTopics {

    // Consumed - chat-service is the source of truth for chat membership.
    public static final String CHAT_CREATED = "chat.created.v1";
    public static final String CHAT_MEMBER_ADDED = "chat.member.added.v1";
    public static final String CHAT_MEMBER_REMOVED = "chat.member.removed.v1";
    public static final String CHAT_DELETED = "chat.deleted.v1";

    // Published - this is the wire contract chat-service already agreed to consume
    // (see chat-service's event.incoming.MessageSentEvent) so its unread counts and
    // last-message previews stay live.
    public static final String MESSAGE_SENT = "message.sent.v1";
    public static final String MESSAGE_DELETED = "message.deleted.v1";

    private MessageTopics() {
    }
}
