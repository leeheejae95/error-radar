package org.errorradar.errorradar.alert.service;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.errorradar.errorradar.global.exception.CustomException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.*;

class AlertServiceTest {

    private MockWebServer mockWebServer;
    private AlertService alertService;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String webhookUrl = mockWebServer.url("/slack/webhook").toString();
        alertService = new AlertService(webhookUrl, 3, 30, RestClient.create());
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("Slack 웹훅 요청이 성공하면 예외가 발생하지 않는다")
    void sendSlackAlert_정상_발송_성공() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));

        assertThatNoException().isThrownBy(() ->
                alertService.sendSlackAlert("user-service", "NPE", "Null pointer", 3L));
    }

    @Test
    @DisplayName("Slack 웹훅 요청에 서비스 정보가 포함된다")
    void sendSlackAlert_요청_바디에_서비스_정보_포함() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        alertService.sendSlackAlert("order-service", "TimeoutException", "Connection timeout", 5L);

        RecordedRequest request = mockWebServer.takeRequest();
        String body = request.getBody().readUtf8();
        assertThat(body).contains("order-service");
        assertThat(body).contains("TimeoutException");
        assertThat(request.getHeader("Content-Type")).contains("application/json");
    }

    @Test
    @DisplayName("Slack 웹훅 요청이 5xx 응답이면 CustomException을 던진다")
    void sendSlackAlert_서버오류_CustomException_발생() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        assertThatThrownBy(() ->
                alertService.sendSlackAlert("user-service", "NPE", "Null pointer", 3L))
                .isInstanceOf(CustomException.class);
    }
}
