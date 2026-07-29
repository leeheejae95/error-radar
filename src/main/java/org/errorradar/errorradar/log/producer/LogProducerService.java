package org.errorradar.errorradar.log.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.errorradar.errorradar.config.KafkaTopicConfig;
import org.errorradar.errorradar.log.dto.LogEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void sendLog(LogEvent event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(KafkaTopicConfig.ERROR_LOG_TOPIC, event.getServiceName(), message)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Kafka 발송 실패 - 서비스: {}, 원인: {}", event.getServiceName(), ex.getMessage());
                        } else {
                            log.info("Kafka 발송 완료 - 서비스: {}, offset: {}",
                                    event.getServiceName(),
                                    result.getRecordMetadata().offset());
                        }
                    });
        } catch (Exception e) {
            log.error("Kafka 메시지 직렬화 실패: {}", e.getMessage());
            throw new RuntimeException("로그 메시지 발송 실패", e);
        }
    }
}
