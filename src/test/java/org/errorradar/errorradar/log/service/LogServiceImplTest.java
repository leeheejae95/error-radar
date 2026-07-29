package org.errorradar.errorradar.log.service;

import org.errorradar.errorradar.log.dto.LogResponse;
import org.errorradar.errorradar.log.entity.ErrorLog;
import org.errorradar.errorradar.log.repository.ErrorLogRepository;
import org.errorradar.errorradar.log.service.impl.LogServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogServiceImplTest {

    @Mock
    private ErrorLogRepository errorLogRepository;

    @InjectMocks
    private LogServiceImpl logService;

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
