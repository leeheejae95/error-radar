package org.errorradar.errorradar.log.repository;

import org.errorradar.errorradar.log.entity.ErrorLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.*;

public interface ErrorLogRepository extends JpaRepository<ErrorLog, Long> {

    // 서비스명, 에러타입으로 조회
    List<ErrorLog> findByServiceNameAndErrorType(String serviceNam, String errorType);

    // 특정시간 이후 발생한 에러 조회
    List<ErrorLog> findByServiceNameAndErrorTypeAndOccurredAtAfter(String serviceName, String errorType, LocalDateTime occurredAt);

    // 알림 발송된 로그 조회
    List<ErrorLog> findByIsAlertedTrue();

    // 서비스명으로 최신순 조회
    List<ErrorLog> findByServiceNameOrderByOccurredAtDesc(String serviceName);
}
