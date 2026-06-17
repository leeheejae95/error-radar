package org.errorradar.errorradar.alert.service;

import lombok.extern.slf4j.Slf4j;
import org.errorradar.errorradar.global.errorcode.ErrorCode;
import org.errorradar.errorradar.global.exception.CustomException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Service
public class AlertService {

    @Value("${alert.slack.webhook-url}")
    private String webhookUrl;

    @Value("${alert.threshold.count}")
    private int thresholdCount;

    @Value("${alert.threshold.window-minutes}")
    private int windowMinutes;

    private final RestClient resetClient = RestClient.create();

    public void sendSlackAlert(String serviceName, String errorType, String errorMessage, long count) {
        String message = buildMessage(serviceName, errorType, errorMessage, count);

        try {
            resetClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("text", message))
                    .retrieve() // 응답받을 준비
                    .toBodilessEntity(); // 알림이 잘 전송됐는지 ok라는 상태코드로만 확인

            log.info("AlertService Slack알림 발송 완료 - 서비스: {}, 에러: {}", serviceName, errorType);
        } catch(Exception e) {
            throw new CustomException(ErrorCode.ALERT_SEND_FAIL);
        }
    }

    private String buildMessage(String serviceName, String errorType, String errorMessage, long count) {
        return String.format("""
                장애 경고 발생!
                
                • *서비스*: %s
                • *에러 타입*: %s
                • *발생 횟수*: %d회 / %d분 이내
                • *에러 메시지*: %s
                
                즉시 확인이 필요합니다!
                """, serviceName, errorType, count, windowMinutes, errorMessage);
    }
}
