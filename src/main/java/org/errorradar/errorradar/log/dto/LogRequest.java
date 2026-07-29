package org.errorradar.errorradar.log.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogRequest {

    private String serviceName; // 서비스명
    private String errorType; // 에러 타입
    private String errorMessage; // 에러 메시지
    private String environment; // 개발,운영횐경 (prod, dev)
}
