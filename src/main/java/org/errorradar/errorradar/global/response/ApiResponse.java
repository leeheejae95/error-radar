package org.errorradar.errorradar.global.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.errorradar.errorradar.global.errorcode.ErrorCode;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {
    private final boolean success;
    private final String code;
    private final String message;
    private final T data;

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "S001", "success", data);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, "S001", message, data);
    }

    public static <T> ApiResponse<T> fail(String code, String message) {
        return new ApiResponse<>(true, code, message, null);
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
        return new ApiResponse<>(false, errorCode.getCode(), errorCode.getMessage(), null);
    }

}
