package com.socialmedia.auth.security.oauth2;

import com.socialmedia.common.exception.BusinessException;
import java.util.Map;
import org.springframework.http.HttpStatus;

public final class OAuth2UserInfoFactory {

    private OAuth2UserInfoFactory() {
    }

    public static OAuth2UserInfo from(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId.toLowerCase()) {
            case "google" -> new GoogleOAuth2UserInfo(attributes);
            case "github" -> new GitHubOAuth2UserInfo(attributes);
            default -> throw new BusinessException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_OAUTH_PROVIDER",
                    "Unsupported OAuth2 provider: " + registrationId);
        };
    }
}
