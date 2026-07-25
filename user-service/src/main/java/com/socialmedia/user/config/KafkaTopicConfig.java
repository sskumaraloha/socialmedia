package com.socialmedia.user.config;

import com.socialmedia.user.event.UserTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic userProfileCreatedTopic() {
        return TopicBuilder.name(UserTopics.USER_PROFILE_CREATED).partitions(6).replicas(1).build();
    }

    @Bean
    public NewTopic userProfileUpdatedTopic() {
        return TopicBuilder.name(UserTopics.USER_PROFILE_UPDATED).partitions(6).replicas(1).build();
    }

    @Bean
    public NewTopic userFollowedTopic() {
        return TopicBuilder.name(UserTopics.USER_FOLLOWED).partitions(6).replicas(1).build();
    }

    @Bean
    public NewTopic userUnfollowedTopic() {
        return TopicBuilder.name(UserTopics.USER_UNFOLLOWED).partitions(6).replicas(1).build();
    }

    @Bean
    public NewTopic userBlockedTopic() {
        return TopicBuilder.name(UserTopics.USER_BLOCKED).partitions(3).replicas(1).build();
    }
}
