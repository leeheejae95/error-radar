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
    ALERT_SEND_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "A001", "Slack 알림 전송에 실패했습니다."),

    // 인증
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "U001", "이미 사용 중인 이메일입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U002", "존재하지 않는 사용자입니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "U003", "비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "U004", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "U005", "만료된 토큰입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "U006", "Refresh Token이 존재하지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "U007", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "U008", "접근 권한이 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
