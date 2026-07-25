package com.socialmedia.media.config;

import com.socialmedia.media.event.MediaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic mediaUploadedTopic() {
        return TopicBuilder.name(MediaTopics.MEDIA_UPLOADED).partitions(6).replicas(1).build();
    }

    @Bean
    public NewTopic mediaProcessedTopic() {
        return TopicBuilder.name(MediaTopics.MEDIA_PROCESSED).partitions(6).replicas(1).build();
    }

    @Bean
    public NewTopic mediaQuarantinedTopic() {
        return TopicBuilder.name(MediaTopics.MEDIA_QUARANTINED).partitions(3).replicas(1).build();
    }
}
