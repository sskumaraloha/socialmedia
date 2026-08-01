package com.socialmedia.search.event;

public final class SearchTopics {

    public static final String USER_PROFILE_CREATED = "user.profile.created.v1";
    public static final String USER_PROFILE_UPDATED = "user.profile.updated.v1";

    public static final String CHAT_CREATED = "chat.created.v1";
    public static final String CHAT_UPDATED = "chat.updated.v1";
    public static final String CHAT_DELETED = "chat.deleted.v1";

    // No message.sent.v1 / message.deleted.v1 subscription: with end-to-end encrypted message
    // content there is nothing in those events this service could index. See SearchIndexNames.

    public static final String MEDIA_UPLOADED = "media.uploaded.v1";
    public static final String MEDIA_DELETED = "media.deleted.v1";
    public static final String MEDIA_QUARANTINED = "media.quarantined.v1";

    private SearchTopics() {
    }
}
