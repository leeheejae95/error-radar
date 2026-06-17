package org.errorradar.errorradar.log.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.errorradar.errorradar.global.response.ApiResponse;
import org.errorradar.errorradar.log.dto.LogRequest;
import org.errorradar.errorradar.log.dto.LogResponse;
import org.errorradar.errorradar.log.service.LogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Log", description = "에러 로그 수집 및 조회 API")
@Slf4j
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;

    @Operation(
            summary = "에러 로그 수집",
            description = "애플리케이션에서 발생한 에러 로그를 수집합니다. 임계치 초과 시 Slack 알림이 발송됩니다."
    )
    @PostMapping("/collect")
    public ResponseEntity<ApiResponse<LogResponse>> collectLog(@RequestBody LogRequest request) {
        log.info("LogController 에러 로그 수집 요청 - 서비스: {}, 에러: {}", request.getServiceName(), request.getErrorType());

        LogResponse logResponse = logService.collectLog(request);

        return ResponseEntity.ok(ApiResponse.ok(logResponse));
    }

    @Operation(
            summary = "전체 에러 로그 조회",
            description = "수집된 전체 에러 로그를 조회합니다."
    )
    @GetMapping("/getLogs")
    public ResponseEntity<ApiResponse<List<LogResponse>>> getLogs() {
        log.info("LogController 전체 에러 로그 요청");

        List<LogResponse> logResponse = logService.getLogs();

        return ResponseEntity.ok(ApiResponse.ok(logResponse));
    }

    @Operation(
            summary = "서비스별 에러 로그 조회",
            description = "특정 서비스의 에러 로그를 최신순으로 조회합니다."
    )
    @GetMapping("/service/{serviceName}")
    public ResponseEntity<ApiResponse<List<LogResponse>>> getLogsByService(@PathVariable String serviceName) {
        log.info("LogController 서비스별 에러로그 조회 요청 - 서비스: {}", serviceName);

        List<LogResponse> logResponses = logService.getLogsByService(serviceName);

        return ResponseEntity.ok(ApiResponse.ok(logResponses));
    }

    @Operation(
            summary = "장애 감지된 로그 조회",
            description = "Slack 알림이 발송된 장애 로그만 조회합니다."
    )
    @GetMapping("/alerted")
    public ResponseEntity<ApiResponse<List<LogResponse>>> getAlertedLogs() {
        log.info("LogController 장애 감지 로그 요청");

        List<LogResponse> logResponses = logService.getAlertedLogs();

        return ResponseEntity.ok(ApiResponse.ok(logResponses));
    }

}
