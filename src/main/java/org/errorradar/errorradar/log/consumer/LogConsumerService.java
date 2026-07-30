package org.errorradar.errorradar.log.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.errorradar.errorradar.alert.service.AlertService;
import org.errorradar.errorradar.config.KafkaTopicConfig;
import org.errorradar.errorradar.global.metrics.ErrorRadarMetrics;
import org.errorradar.errorradar.log.dto.LogEvent;
import org.errorradar.errorradar.log.entity.ErrorLog;
import org.errorradar.errorradar.log.repository.ErrorLogRepository;
import org.errorradar.errorradar.pattern.service.PatternDetectService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogConsumerService {

    private final ErrorLogRepository errorLogRepository;
    private final PatternDetectService patternDetectService;
    private final AlertService alertService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopicConfig.ERROR_LOG_TOPIC, groupId = "error-radar-group")
    public void consume(String message) {
        try {
            LogEvent event = objectMapper.readValue(message, LogEvent.class);
            log.info("Kafka 메시지 수신 - 서비스: {}, 에러: {}", event.getServiceName(), event.getErrorType());
            processLog(event);
        } catch (Exception e) {
            log.error("Kafka 메시지 처리 실패: {}", e.getMessage(), e);
        }
    }

    private void processLog(LogEvent event) {
        ErrorLog errorLog = ErrorLog.builder()
                .serviceName(event.getServiceName())
                .errorType(event.getErrorType())
                .errorMessage(event.getErrorMessage())
                .environment(event.getEnvironment())
                .build();
        ErrorLog savedLog = errorLogRepository.save(errorLog);

        log.info("에러 로그 DB 저장 완료 - 서비스: {}, 에러: {}", event.getServiceName(), event.getErrorType());

        boolean isThresholdExceeded = patternDetectService.incrementAndCheckThreshold(
                event.getServiceName(), event.getErrorType());

        if (isThresholdExceeded) {
            long count = patternDetectService.getErrorCount(event.getServiceName(), event.getErrorType());

            log.info("임계치 초과 - 서비스: {}, 에러: {}, 횟수: {}", event.getServiceName(), event.getErrorType(), count);

            alertService.sendSlackAlert(event.getServiceName(), event.getErrorType(), event.getErrorMessage(), count);
            savedLog.markAsAlerted();
            patternDetectService.resetCount(event.getServiceName(), event.getErrorType());
        }
    }
}
