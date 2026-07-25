package com.socialmedia.message.config;

import com.socialmedia.message.event.MessageTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic messageSentTopic() {
        return TopicBuilder.name(MessageTopics.MESSAGE_SENT).partitions(12).replicas(1).build();
    }

    @Bean
    public NewTopic messageDeletedTopic() {
        return TopicBuilder.name(MessageTopics.MESSAGE_DELETED).partitions(6).replicas(1).build();
    }
}
