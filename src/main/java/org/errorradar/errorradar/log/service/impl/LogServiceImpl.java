package org.errorradar.errorradar.log.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.errorradar.errorradar.alert.service.AlertService;
import org.errorradar.errorradar.log.dto.LogRequest;
import org.errorradar.errorradar.log.dto.LogResponse;
import org.errorradar.errorradar.global.errorcode.ErrorCode;
import org.errorradar.errorradar.global.exception.CustomException;
import org.errorradar.errorradar.log.entity.ErrorLog;
import org.errorradar.errorradar.log.repository.ErrorLogRepository;
import org.errorradar.errorradar.pattern.service.PatternDetectService;
import org.errorradar.errorradar.log.service.LogService;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogServiceImpl implements LogService {

    private final ErrorLogRepository errorLogRepository;
    private final PatternDetectService patternDetectService;
    private final AlertService alertService;

    @Override
    @Transactional
    public LogResponse collectLog(LogRequest request) {

        validateRequest(request);

        // 1. MySQL에 저장
        ErrorLog errorLog = ErrorLog.builder()
                .serviceName(request.getServiceName())
                .errorType(request.getErrorType())
                .errorMessage(request.getErrorMessage())
                .environment(request.getEnvironment())
                .build();

        ErrorLog saveLog = errorLogRepository.save(errorLog);

        log.info("LogServiceImpl 에러로그 저장 완료 - 서비스 : {}, 에러 : {}", request.getServiceName(), request.getErrorType());

        // 2. Redis에러 카운트 증가 및 임계치 확인 여부
        boolean isThresholdExceeded = patternDetectService.incrementAndCheckThreshold(request.getServiceName(), request.getErrorType());

        // 3. 임계치 초과시 Slack에 알림 발송
        if(isThresholdExceeded) {
            long count = patternDetectService.getErrorCount(request.getServiceName(), request.getErrorType());

            log.info("LogService 임계치 초과 서비스: {}, 에러: {}, 횟수: {}", request.getServiceName(), request.getErrorType(), count);

            // Slack 알림 발송
            alertService.sendSlackAlert(request.getServiceName(), request.getErrorType(), request.getErrorMessage(), count);

            // 로그 일림 발송 완료 처리
            saveLog.markAsAlerted();

            // Redis 카운트 초기화 ( 중복방지 )
            patternDetectService.resetCount(request.getServiceName(), request.getErrorType());
        }

        return LogResponse.from(saveLog);
    }

    @Override
    public List<LogResponse> getLogs() {
        return errorLogRepository.findAll()
                .stream()
                .map(LogResponse::from)
                .toList();
    }

    @Override
    public List<LogResponse> getLogsByService(String serviceName) {
        return errorLogRepository.findByServiceNameOrderByOccurredAtDesc(serviceName)
                .stream()
                .map(LogResponse::from) // 각 에러로그 LogResponse로 변환
                .toList();
    }

    @Override
    public List<LogResponse> getAlertedLogs() {
        return errorLogRepository.findByIsAlertedTrue()
                .stream()
                .map(LogResponse::from)
                .toList();
    }

    private void validateRequest(LogRequest request) {
        if (request.getServiceName() == null || request.getServiceName().isBlank()) {
            throw new CustomException(ErrorCode.LOG_SERVICE_NOT_FOUND);
        }
        if (request.getErrorType() == null || request.getErrorType().isBlank()) {
            throw new CustomException(ErrorCode.INVALID_LOG_LEVEL);
        }
        if (request.getErrorMessage() == null || request.getErrorMessage().isBlank()) {
            throw new CustomException(ErrorCode.LOG_MESSAGE_NOT_FOUND);
        }
    }
}
