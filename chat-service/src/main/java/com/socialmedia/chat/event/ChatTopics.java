package com.socialmedia.chat.event;

public final class ChatTopics {

    // Consumed - published by message-service once it exists; the contract is agreed
    // up front so chat-service can keep unread counts / last-message previews live.
    public static final String MESSAGE_SENT = "message.sent.v1";

    // Published
    public static final String CHAT_CREATED = "chat.created.v1";
    public static final String CHAT_UPDATED = "chat.updated.v1";
    public static final String CHAT_MEMBER_ADDED = "chat.member.added.v1";
    public static final String CHAT_MEMBER_REMOVED = "chat.member.removed.v1";
    public static final String CHAT_DELETED = "chat.deleted.v1";
    public static final String CHAT_MESSAGE_PINNED = "chat.message.pinned.v1";

    private ChatTopics() {
    }
}
