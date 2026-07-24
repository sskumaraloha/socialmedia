package com.socialmedia.auth.event;

import com.socialmedia.auth.domain.User;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes to Kafka as a best-effort side effect of the write that already committed
 * to Postgres - a publish failure is logged, never allowed to fail the HTTP response,
 * since the source of truth (the DB row) is already durable.
 *
 * Partition key = userId (or email pre-authentication) so every event for one user stays
 * ordered on a single partition, matching the platform-wide convention.
 */
@Component
public class AuthEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(AuthEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public AuthEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishUserRegistered(User user) {
        send(AuthTopics.USER_REGISTERED, user.getId().toString(),
                new UserRegisteredEvent(user.getId(), user.getEmail(), user.getProvider().name(), Instant.now()));
    }

    public void publishEmailVerificationRequested(UUID userId, String email, String verificationToken) {
        send(AuthTopics.EMAIL_VERIFICATION_REQUESTED, userId.toString(),
                new EmailVerificationRequestedEvent(userId, email, verificationToken, Instant.now()));
    }

    public void publishUserLoggedIn(UUID userId, String deviceId, String ipAddress) {
        send(AuthTopics.USER_LOGGED_IN, userId.toString(),
                new UserLoggedInEvent(userId, deviceId, ipAddress, Instant.now()));
    }

    public void publishLoginFailed(String email, String ipAddress, String reason) {
        send(AuthTopics.LOGIN_FAILED, email, new LoginFailedEvent(email, ipAddress, reason, Instant.now()));
    }

    public void publishPasswordResetRequested(UUID userId, String email, String resetToken) {
        send(AuthTopics.PASSWORD_RESET_REQUESTED, userId.toString(),
                new PasswordResetRequestedEvent(userId, email, resetToken, Instant.now()));
    }

    public void publishSessionRevoked(UUID userId, UUID sessionId, String deviceId, String reason) {
        send(AuthTopics.SESSION_REVOKED, userId.toString(),
                new SessionRevokedEvent(userId, sessionId, deviceId, reason, Instant.now()));
    }

    public void publishTwoFactorChanged(UUID userId, boolean enabled) {
        send(AuthTopics.TWO_FACTOR_CHANGED, userId.toString(),
                new TwoFactorChangedEvent(userId, enabled, Instant.now()));
    }

    private void send(String topic, String key, Object payload) {
        // producer.send() can throw synchronously (e.g. a metadata-fetch timeout when the
        // broker is unreachable) rather than only ever failing the returned future - both
        // paths must be swallowed here, or a Kafka outage would take down every write path
        // that publishes an event (registration, login, password reset, ...).
        try {
            kafkaTemplate.send(topic, key, payload).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.warn("Failed to publish event to topic {} (key={}): {}", topic, key, ex.getMessage());
                }
            });
        } catch (Exception ex) {
            log.warn("Failed to publish event to topic {} (key={}): {}", topic, key, ex.getMessage());
        }
    }
}
