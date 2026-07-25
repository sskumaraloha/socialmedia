package com.socialmedia.notification.config;

import com.socialmedia.notification.event.NotificationTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic notificationSendRequestedTopic() {
        return TopicBuilder.name(NotificationTopics.NOTIFICATION_SEND_REQUESTED).partitions(6).replicas(1).build();
    }

    @Bean
    public NewTopic notificationSendRequestedDlt() {
        return TopicBuilder.name(NotificationTopics.NOTIFICATION_SEND_REQUESTED + ".DLT").partitions(6).replicas(1).build();
    }

    @Bean
    public NewTopic emailVerificationRequestedDlt() {
        return TopicBuilder.name(NotificationTopics.EMAIL_VERIFICATION_REQUESTED + ".DLT").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic passwordResetRequestedDlt() {
        return TopicBuilder.name(NotificationTopics.PASSWORD_RESET_REQUESTED + ".DLT").partitions(3).replicas(1).build();
    }
}
