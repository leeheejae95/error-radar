package org.errorradar.errorradar.log.service;

import org.errorradar.errorradar.log.dto.LogRequest;
import org.errorradar.errorradar.log.dto.LogResponse;

import java.util.List;

public interface LogService {

    /**
     * 에러 로그 수집 및 패턴 감지
     * @param request 에러 로그 요청
     * @return 저장된 로그 응답
     */
    LogResponse collectLog(LogRequest request);

    /**
     * 전체 에러 로그 조회
     */
    List<LogResponse> getLogs();

    /**
     * 서비스명으로 에러 로그 조회
     */
    List<LogResponse> getLogsByService(String serviceName);

    /**
     * 장애 감지된 로그 조회
     */
    List<LogResponse> getAlertedLogs();
}
