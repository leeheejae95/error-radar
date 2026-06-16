package org.errorradar.errorradar.dto;

import lombok.Builder;
import lombok.Getter;
import org.errorradar.errorradar.log.entity.ErrorLog;

import java.time.LocalDateTime;

@Getter
@Builder
public class LogResponse {

    private Long id;
    private String serviceName;
    private String errorType;
    private String errorMessage;
    private String environment;
    private LocalDateTime occurredAt;

    public static LogResponse from(ErrorLog errorLog) {
        return LogResponse.builder()
                .id(errorLog.getId())
                .serviceName(errorLog.getServiceName())
                .errorType(errorLog.getErrorType())
                .environment(errorLog.getEnvironment())
                .occurredAt(errorLog.getOccurredAt())
                .build();
    }
}
