package com.socialmedia.chat.client;

import com.socialmedia.chat.exception.UserServiceUnavailableException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * The external validation step of GroupChatCreationSaga: user-service's GET /users/{id}
 * already 404s an unknown user (see UserProfileServiceImpl.require), so reusing it here
 * avoids chat-service duplicating user-service's notion of what a valid user id is.
 */
@Component
public class UserExistenceClient {

    private static final Logger log = LoggerFactory.getLogger(UserExistenceClient.class);

    private final RestClient restClient;

    public UserExistenceClient(@Value("${services.user-service.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public boolean exists(UUID userId, String bearerAuthorizationHeader) {
        try {
            restClient.get()
                    .uri("/api/v1/users/{userId}", userId)
                    .header("Authorization", bearerAuthorizationHeader)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        } catch (Exception e) {
            log.error("Failed to verify user existence via user-service for user {}: {}", userId, e.getMessage());
            throw new UserServiceUnavailableException();
        }
    }
}
