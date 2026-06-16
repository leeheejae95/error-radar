package org.errorradar.errorradar.global.exception;

import lombok.Getter;
import org.errorradar.errorradar.global.errorcode.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
public class CustomException extends RuntimeException{

    private final HttpStatus httpStatus;
    private final String code;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.httpStatus = errorCode.getStatus();
        this.code = errorCode.getCode();
    }

    public CustomException(HttpStatus status, String code, String message) {
        super(message);
        this.httpStatus = status;
        this.code = code;
    }
}
