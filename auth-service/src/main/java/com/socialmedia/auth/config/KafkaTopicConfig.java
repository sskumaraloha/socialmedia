package com.socialmedia.auth.config;

import com.socialmedia.auth.event.AuthTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic userRegisteredTopic() {
        return TopicBuilder.name(AuthTopics.USER_REGISTERED).partitions(6).replicas(1).build();
    }

    @Bean
    public NewTopic emailVerificationRequestedTopic() {
        return TopicBuilder.name(AuthTopics.EMAIL_VERIFICATION_REQUESTED).partitions(6).replicas(1).build();
    }

    @Bean
    public NewTopic userLoggedInTopic() {
        return TopicBuilder.name(AuthTopics.USER_LOGGED_IN).partitions(6).replicas(1).build();
    }

    @Bean
    public NewTopic loginFailedTopic() {
        return TopicBuilder.name(AuthTopics.LOGIN_FAILED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic passwordResetRequestedTopic() {
        return TopicBuilder.name(AuthTopics.PASSWORD_RESET_REQUESTED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic sessionRevokedTopic() {
        return TopicBuilder.name(AuthTopics.SESSION_REVOKED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic twoFactorChangedTopic() {
        return TopicBuilder.name(AuthTopics.TWO_FACTOR_CHANGED).partitions(3).replicas(1).build();
    }
}
