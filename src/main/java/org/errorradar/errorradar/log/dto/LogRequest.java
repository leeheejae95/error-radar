package org.errorradar.errorradar.log.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogRequest {
    @NotBlank(message = "서비스명은 필수입니다.")
    private String serviceName;

    @NotBlank(message = "에러 타입은 필수입니다.")
    private String errorType;

    @NotBlank(message = "에러 메시지는 필수입니다.")
    private String errorMessage;

    private String environment;
}
