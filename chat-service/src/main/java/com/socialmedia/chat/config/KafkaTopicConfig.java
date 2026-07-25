package com.socialmedia.chat.config;

import com.socialmedia.chat.event.ChatTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic chatCreatedTopic() {
        return TopicBuilder.name(ChatTopics.CHAT_CREATED).partitions(6).replicas(1).build();
    }

    @Bean
    public NewTopic chatUpdatedTopic() {
        return TopicBuilder.name(ChatTopics.CHAT_UPDATED).partitions(6).replicas(1).build();
    }

    @Bean
    public NewTopic chatMemberAddedTopic() {
        return TopicBuilder.name(ChatTopics.CHAT_MEMBER_ADDED).partitions(6).replicas(1).build();
    }

    @Bean
    public NewTopic chatMemberRemovedTopic() {
        return TopicBuilder.name(ChatTopics.CHAT_MEMBER_REMOVED).partitions(6).replicas(1).build();
    }

    @Bean
    public NewTopic chatDeletedTopic() {
        return TopicBuilder.name(ChatTopics.CHAT_DELETED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic chatMessagePinnedTopic() {
        return TopicBuilder.name(ChatTopics.CHAT_MESSAGE_PINNED).partitions(6).replicas(1).build();
    }
}
