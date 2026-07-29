package org.errorradar.errorradar.integration;

import org.awaitility.Awaitility;
import org.errorradar.errorradar.alert.service.AlertService;
import org.errorradar.errorradar.log.dto.LogRequest;
import org.errorradar.errorradar.log.entity.ErrorLog;
import org.errorradar.errorradar.log.repository.ErrorLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        topics = "error-logs",
        bootstrapServersProperty = "spring.kafka.bootstrap-servers",
        brokerProperties = {"log.cleaner.enable=false"}
)
class LogIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.2-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ErrorLogRepository errorLogRepository;

    @MockitoBean
    private AlertService alertService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        errorLogRepository.deleteAll();
        doNothing().when(alertService).sendSlackAlert(anyString(), anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("로그 수집 API 호출 시 202를 반환하고 비동기로 DB 저장")
    void 로그_수집_API_성공_비동기_저장() throws Exception {
        LogRequest request = LogRequest.builder()
                .serviceName("user-service")
                .errorType("NullPointerException")
                .errorMessage("Null pointer occurred")
                .environment("prod")
                .build();

        mockMvc.perform(post("/api/logs/collect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data").value("로그가 접수되었습니다."));

        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(1))
                .until(() -> errorLogRepository.count() == 1);
    }

    @Test
    @DisplayName("서비스명 없이 요청 시 400 반환")
    void 로그_수집_서비스명_누락_400_반환() throws Exception {
        LogRequest request = LogRequest.builder()
                .serviceName("")
                .errorType("NPE")
                .errorMessage("error")
                .build();

        mockMvc.perform(post("/api/logs/collect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("전체 로그 조회 API가 저장된 모든 로그 반환")
    void 전체_로그_조회_API_성공() throws Exception {
        saveErrorLog("user-service", "NPE", false);
        saveErrorLog("order-service", "TimeoutException", false);

        mockMvc.perform(get("/api/logs/getLogs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    @DisplayName("서비스별 로그 조회 API가 해당 서비스 로그 반환")
    void 서비스별_로그_조회_API_성공() throws Exception {
        saveErrorLog("user-service", "NPE", false);
        saveErrorLog("order-service", "TimeoutException", false);

        mockMvc.perform(get("/api/logs/service/user-service"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].serviceName").value("user-service"));
    }

    @Test
    @DisplayName("알림 발송된 로그 조회 API가 alerted 로그반환")
    void 알림발송_로그_조회_API_성공() throws Exception {
        saveErrorLog("user-service", "NPE", false);
        saveErrorLog("order-service", "TimeoutException", true);

        mockMvc.perform(get("/api/logs/alerted"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].serviceName").value("order-service"));
    }

    @Test
    @DisplayName("임계치(3회) 도달 시 알림 발송")
    void 임계치_초과시_Kafka_비동기_알림_발송() throws Exception {
        LogRequest request = LogRequest.builder()
                .serviceName("payment-service")
                .errorType("DatabaseException")
                .errorMessage("DB connection failed")
                .environment("prod")
                .build();

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/logs/collect")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isAccepted());
        }

        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(1))
                .until(() -> errorLogRepository.count() == 3);
    }

    private void saveErrorLog(String serviceName, String errorType, boolean isAlerted) {
        ErrorLog log = ErrorLog.builder()
                .serviceName(serviceName)
                .errorType(errorType)
                .errorMessage("test error message")
                .environment("prod")
                .build();
        if (isAlerted) log.markAsAlerted();
        errorLogRepository.save(log);
    }
}
