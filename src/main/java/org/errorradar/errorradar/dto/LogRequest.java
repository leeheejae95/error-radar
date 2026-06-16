package org.errorradar.errorradar.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LogRequest {

    private String serviceName; // 서비스명
    private String errorType; // 에러 타입
    private String errorMessage; // 에러 메시지
    private String environment; // 개발,운영횐경 (prod, dev)
}
