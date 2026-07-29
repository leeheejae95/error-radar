package org.errorradar.errorradar.log.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogEvent {

    private String serviceName;
    private String errorType;
    private String errorMessage;
    private String environment;
}
