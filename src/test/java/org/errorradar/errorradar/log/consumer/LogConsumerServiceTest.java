package org.errorradar.errorradar.log.consumer;

import org.errorradar.errorradar.alert.service.AlertService;
import org.errorradar.errorradar.global.exception.CustomException;
import org.errorradar.errorradar.log.dto.LogEvent;
import org.errorradar.errorradar.log.entity.ErrorLog;
import org.errorradar.errorradar.log.repository.ErrorLogRepository;
import org.errorradar.errorradar.pattern.service.PatternDetectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogConsumerServiceTest {

    @Mock
    private ErrorLogRepository errorLogRepository;

    @Mock
    private PatternDetectService patternDetectService;

    @Mock
    private AlertService alertService;

    @InjectMocks
    private LogConsumerService logConsumerService;

    private String validMessage;
    private ErrorLog savedLog;

    @BeforeEach
    void setUp() throws Exception {
        ReflectionTestUtils.setField(logConsumerService, "objectMapper", new ObjectMapper());

        LogEvent event = LogEvent.builder()
                .serviceName("user-service")
                .errorType("NullPointerException")
                .errorMessage("Null pointer occurred")
                .environment("prod")
                .build();
        validMessage = new ObjectMapper().writeValueAsString(event);

        savedLog = ErrorLog.builder()
                .serviceName("user-service")
                .errorType("NullPointerException")
                .errorMessage("Null pointer occurred")
                .environment("prod")
                .build();
        ReflectionTestUtils.setField(savedLog, "id", 1L);
    }

    @Test
    @DisplayName("임계치 미달 시 DB에 저장하고 알림을 발송하지 않는다")
    void consume_임계치_미달_알림_미발송() {
        when(errorLogRepository.save(any(ErrorLog.class))).thenReturn(savedLog);
        when(patternDetectService.incrementAndCheckThreshold(anyString(), anyString())).thenReturn(false);

        logConsumerService.consume(validMessage);

        verify(errorLogRepository).save(any(ErrorLog.class));
        verify(alertService, never()).sendSlackAlert(anyString(), anyString(), anyString(), anyLong());
        verify(patternDetectService, never()).resetCount(anyString(), anyString());
    }

    @Test
    @DisplayName("임계치 초과 시 Slack 알림을 발송하고 카운트를 초기화한다")
    void consume_임계치_초과_Slack_알림_발송() {
        when(errorLogRepository.save(any(ErrorLog.class))).thenReturn(savedLog);
        when(patternDetectService.incrementAndCheckThreshold(anyString(), anyString())).thenReturn(true);
        when(patternDetectService.getErrorCount(anyString(), anyString())).thenReturn(3L);

        logConsumerService.consume(validMessage);

        verify(alertService).sendSlackAlert("user-service", "NullPointerException", "Null pointer occurred", 3L);
        verify(patternDetectService).resetCount("user-service", "NullPointerException");
    }

    @Test
    @DisplayName("잘못된 메시지가 들어와도 예외를 던지지 않고 로깅한다")
    void consume_잘못된_메시지_예외_미전파() {
        assertThatNoException().isThrownBy(() ->
                logConsumerService.consume("invalid-json-message"));
    }
}
