package com.socialmedia.auth.event;

public final class AuthTopics {

    public static final String USER_REGISTERED = "auth.user.registered.v1";
    public static final String EMAIL_VERIFICATION_REQUESTED = "auth.email-verification-requested.v1";
    public static final String USER_LOGGED_IN = "auth.user.logged-in.v1";
    public static final String LOGIN_FAILED = "auth.login.failed.v1";
    public static final String PASSWORD_RESET_REQUESTED = "auth.password.reset-requested.v1";
    public static final String SESSION_REVOKED = "auth.session.revoked.v1";
    public static final String TWO_FACTOR_CHANGED = "auth.user.2fa-changed.v1";

    private AuthTopics() {
    }
}
