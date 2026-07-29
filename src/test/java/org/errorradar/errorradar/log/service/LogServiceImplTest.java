package org.errorradar.errorradar.log.service;

import org.errorradar.errorradar.alert.service.AlertService;
import org.errorradar.errorradar.global.exception.CustomException;
import org.errorradar.errorradar.log.dto.LogRequest;
import org.errorradar.errorradar.log.dto.LogResponse;
import org.errorradar.errorradar.log.entity.ErrorLog;
import org.errorradar.errorradar.log.repository.ErrorLogRepository;
import org.errorradar.errorradar.log.service.impl.LogServiceImpl;
import org.errorradar.errorradar.pattern.service.PatternDetectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogServiceImplTest {

    @Mock
    private ErrorLogRepository errorLogRepository;

    @Mock
    private PatternDetectService patternDetectService;

    @Mock
    private AlertService alertService;

    @InjectMocks
    private LogServiceImpl logService;

    private LogRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = LogRequest.builder()
                .serviceName("user-service")
                .errorType("NullPointerException")
                .errorMessage("Null pointer occurred")
                .environment("prod")
                .build();
    }

    @Test
    @DisplayName("임계치 미달 시 로그를 저장하고 알림을 발송하지 않는다")
    void collectLog_임계치_미달_알림_미발송() {
        ErrorLog savedLog = buildErrorLog(1L);
        when(errorLogRepository.save(any(ErrorLog.class))).thenReturn(savedLog);
        when(patternDetectService.incrementAndCheckThreshold(anyString(), anyString())).thenReturn(false);

        LogResponse response = logService.collectLog(validRequest);

        assertThat(response).isNotNull();
        assertThat(response.getServiceName()).isEqualTo("user-service");
        verify(alertService, never()).sendSlackAlert(anyString(), anyString(), anyString(), anyLong());
        verify(patternDetectService, never()).resetCount(anyString(), anyString());
    }

    @Test
    @DisplayName("임계치 초과 시 Slack 알림을 발송하고 카운트를 초기화한다")
    void collectLog_임계치_초과_Slack_알림_발송_및_카운트_초기화() {
        ErrorLog savedLog = buildErrorLog(1L);
        when(errorLogRepository.save(any(ErrorLog.class))).thenReturn(savedLog);
        when(patternDetectService.incrementAndCheckThreshold(anyString(), anyString())).thenReturn(true);
        when(patternDetectService.getErrorCount(anyString(), anyString())).thenReturn(3L);

        logService.collectLog(validRequest);

        verify(alertService).sendSlackAlert("user-service", "NullPointerException", "Null pointer occurred", 3L);
        verify(patternDetectService).resetCount("user-service", "NullPointerException");
    }

    @Test
    @DisplayName("임계치 초과 시 로그의 알림 발송 상태를 true로 변경한다")
    void collectLog_임계치_초과시_alerted_상태_변경() {
        ErrorLog savedLog = buildErrorLog(1L);
        when(errorLogRepository.save(any(ErrorLog.class))).thenReturn(savedLog);
        when(patternDetectService.incrementAndCheckThreshold(anyString(), anyString())).thenReturn(true);
        when(patternDetectService.getErrorCount(anyString(), anyString())).thenReturn(3L);

        logService.collectLog(validRequest);

        assertThat(savedLog.isAlerted()).isTrue();
    }

    @Test
    @DisplayName("서비스명이 비어있으면 CustomException을 던진다")
    void collectLog_서비스명_누락시_예외_발생() {
        LogRequest request = LogRequest.builder()
                .serviceName("")
                .errorType("NPE")
                .errorMessage("error")
                .build();

        assertThatThrownBy(() -> logService.collectLog(request))
                .isInstanceOf(CustomException.class);
        verify(errorLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("에러 타입이 비어있으면 CustomException을 던진다")
    void collectLog_에러타입_누락시_예외_발생() {
        LogRequest request = LogRequest.builder()
                .serviceName("user-service")
                .errorType("")
                .errorMessage("error")
                .build();

        assertThatThrownBy(() -> logService.collectLog(request))
                .isInstanceOf(CustomException.class);
        verify(errorLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("에러 메시지가 비어있으면 CustomException을 던진다")
    void collectLog_에러메시지_누락시_예외_발생() {
        LogRequest request = LogRequest.builder()
                .serviceName("user-service")
                .errorType("NPE")
                .errorMessage("")
                .build();

        assertThatThrownBy(() -> logService.collectLog(request))
                .isInstanceOf(CustomException.class);
        verify(errorLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("전체 로그를 조회하면 저장된 모든 로그를 반환한다")
    void getLogs_전체_로그_반환() {
        when(errorLogRepository.findAll()).thenReturn(
                List.of(buildErrorLog(1L), buildErrorLog(2L)));

        List<LogResponse> result = logService.getLogs();

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("서비스명으로 조회하면 해당 서비스의 로그를 최신순으로 반환한다")
    void getLogsByService_서비스명으로_최신순_조회() {
        when(errorLogRepository.findByServiceNameOrderByOccurredAtDesc("user-service"))
                .thenReturn(List.of(buildErrorLog(2L), buildErrorLog(1L)));

        List<LogResponse> result = logService.getLogsByService("user-service");

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("알림 발송된 로그만 조회된다")
    void getAlertedLogs_알림발송_로그만_반환() {
        ErrorLog alertedLog = buildErrorLog(1L);
        alertedLog.markAsAlerted();
        when(errorLogRepository.findByIsAlertedTrue()).thenReturn(List.of(alertedLog));

        List<LogResponse> result = logService.getAlertedLogs();

        assertThat(result).hasSize(1);
    }

    private ErrorLog buildErrorLog(Long id) {
        ErrorLog log = ErrorLog.builder()
                .serviceName("user-service")
                .errorType("NullPointerException")
                .errorMessage("Null pointer occurred")
                .environment("prod")
                .build();
        ReflectionTestUtils.setField(log, "id", id);
        return log;
    }
}
