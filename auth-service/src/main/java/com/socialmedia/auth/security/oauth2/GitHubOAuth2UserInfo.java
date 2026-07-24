package com.socialmedia.auth.security.oauth2;

import java.util.Map;

public class GitHubOAuth2UserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    public GitHubOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProviderId() {
        return String.valueOf(attributes.get("id"));
    }

    @Override
    public String getEmail() {
        Object email = attributes.get("email");
        if (email != null) {
            return String.valueOf(email);
        }
        // GitHub only returns a public email if the user has one set; fall back to a
        // stable synthetic address keyed on their immutable numeric id.
        return getProviderId() + "+" + attributes.get("login") + "@users.noreply.github.com";
    }

    @Override
    public String getName() {
        Object name = attributes.get("name");
        return name != null ? String.valueOf(name) : String.valueOf(attributes.get("login"));
    }
}
