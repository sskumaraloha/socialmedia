package com.socialmedia.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Every service that validates (but doesn't issue) access tokens shares this one
 * property with auth-service: the HMAC signing secret, via the JWT_SECRET env var. Only
 * auth-service knows the token TTLs / issuance details - resource servers just verify.
 */
@ConfigurationProperties(prefix = "app.security.jwt")
public class SharedJwtProperties {

    private String secret = "change-me-in-env-this-is-not-a-real-secret-change-me-please";

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}
