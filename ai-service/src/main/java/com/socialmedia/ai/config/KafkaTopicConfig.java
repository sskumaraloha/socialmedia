package com.socialmedia.ai.config;

import com.socialmedia.ai.event.AiTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic messageModeratedTopic() {
        return TopicBuilder.name(AiTopics.MESSAGE_MODERATED).partitions(6).replicas(1).build();
    }
}
