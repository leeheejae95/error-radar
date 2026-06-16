package org.errorradar.errorradar.global.errorcode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 공통
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버 오류가 발생했습니다."),

    // 로그
    LOG_NOT_FOUND(HttpStatus.NOT_FOUND, "L001", "로그를 찾을 수 없습니다."),
    INVALID_LOG_LEVEL(HttpStatus.BAD_REQUEST, "L002", "유효하지 않은 로그 레벨입니다."),
    LOG_SERVICE_NOT_FOUND(HttpStatus.BAD_REQUEST, "L003", "서비스명을 입력해주세요."),
    LOG_MESSAGE_NOT_FOUND(HttpStatus.BAD_REQUEST, "L004", "로그 메시지를 입력해주세요."),

    // 알림
    ALERT_SEND_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "A001", "Slack 알림 전송에 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
