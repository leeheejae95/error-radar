package org.errorradar.errorradar.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String ERROR_LOG_TOPIC = "error-logs";

    @Bean
    public NewTopic errorLogTopic() {
        return TopicBuilder.name(ERROR_LOG_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
