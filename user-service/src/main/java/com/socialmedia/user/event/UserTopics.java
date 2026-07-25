package com.socialmedia.user.event;

public final class UserTopics {

    // Consumed
    public static final String AUTH_USER_REGISTERED = "auth.user.registered.v1";
    public static final String PRESENCE_CHANGED = "presence.changed.v1";

    // Published
    public static final String USER_PROFILE_CREATED = "user.profile.created.v1";
    public static final String USER_PROFILE_UPDATED = "user.profile.updated.v1";
    public static final String USER_FOLLOWED = "user.followed.v1";
    public static final String USER_UNFOLLOWED = "user.unfollowed.v1";
    public static final String USER_BLOCKED = "user.blocked.v1";

    private UserTopics() {
    }
}
