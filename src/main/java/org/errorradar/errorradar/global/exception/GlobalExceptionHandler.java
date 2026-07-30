package org.errorradar.errorradar.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.errorradar.errorradar.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("입력값이 올바르지 않습니다.");
        log.warn("Validation failed: {}", message);
        return ResponseEntity.badRequest().body(ApiResponse.fail("V001", message));
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handlerCustomException(CustomException e) {
        log.error("CustomException code : {}, message : {}", e.getCode(), e.getMessage());
        return ResponseEntity
                .status(e.getHttpStatus())
                .body(ApiResponse.fail(e.getCode(), e.getMessage()));
    }

    public ResponseEntity<ApiResponse<Void>> handlerException(Exception e) {
        log.error("Exception {}", e.getMessage(), e);
        return ResponseEntity
                .internalServerError()
                .body(ApiResponse.fail("C002", "서버 오류가 발생했습니다."));
    }
}
