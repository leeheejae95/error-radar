package org.errorradar.errorradar.global.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class ErrorRadarMetrics {

    private final Counter logCollectedCounter;
    private final Counter alertSentCounter;
    private final Counter loginSuccessCounter;
    private final Counter loginFailCounter;

    public ErrorRadarMetrics(MeterRegistry registry) {
        this.logCollectedCounter = Counter.builder("errorradar.log.collected")
                .description("수집된 에러 로그 총 횟수")
                .register(registry);

        this.alertSentCounter = Counter.builder("errorradar.alert.sent")
                .description("Slack 알림 발송 횟수")
                .register(registry);

        this.loginSuccessCounter = Counter.builder("errorradar.auth.login.success")
                .description("로그인 성공 횟수")
                .register(registry);

        this.loginFailCounter = Counter.builder("errorradar.auth.login.fail")
                .description("로그인 실패 횟수")
                .register(registry);
    }

    // 이벤트 발생시 카운트 증가
    public void incrementLogCollected() { logCollectedCounter.increment(); }
    public void incrementAlertSent()    { alertSentCounter.increment(); }
    public void incrementLoginSuccess() { loginSuccessCounter.increment(); }
    public void incrementLoginFail()    { loginFailCounter.increment(); }
}
