package com.socialmedia.auth.security.oauth2;

/**
 * Normalizes the differing attribute shapes Google and GitHub return for
 * /oauth2/userinfo into a single contract the rest of the app depends on.
 */
public interface OAuth2UserInfo {

    String getProviderId();

    String getEmail();

    String getName();
}
