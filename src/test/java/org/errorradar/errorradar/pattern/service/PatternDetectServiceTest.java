package org.errorradar.errorradar.pattern.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class PatternDetectServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private PatternDetectService patternDetectService;

    private static final String SERVICE_NAME = "user-service";
    private static final String ERROR_TYPE = "NullPointerException";
    private static final String EXPECTED_KEY = "error:count:user-service:NullPointerException";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(patternDetectService, "thresholdCount", 3);
        ReflectionTestUtils.setField(patternDetectService, "windowMinutes", 30);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("첫 번째 에러 발생 시 TTL을 설정하고 임계치 미달이면 false를 반환한다")
    void 첫번째_에러_발생시_TTL_설정_임계치_미달() {
        when(valueOperations.increment(EXPECTED_KEY)).thenReturn(1L);

        boolean result = patternDetectService.incrementAndCheckThreshold(SERVICE_NAME, ERROR_TYPE);

        assertThat(result).isFalse();
        verify(redisTemplate).expire(eq(EXPECTED_KEY), any(Duration.class));
    }

    @Test
    @DisplayName("두 번째 이후 에러는 TTL을 재설정하지 않는다")
    void 두번째_이후_에러_TTL_미설정() {
        when(valueOperations.increment(EXPECTED_KEY)).thenReturn(2L);

        patternDetectService.incrementAndCheckThreshold(SERVICE_NAME, ERROR_TYPE);

        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("카운트가 임계치에 정확히 도달하면 true를 반환한다")
    void 카운트_임계치_도달시_true_반환() {
        when(valueOperations.increment(EXPECTED_KEY)).thenReturn(3L);

        boolean result = patternDetectService.incrementAndCheckThreshold(SERVICE_NAME, ERROR_TYPE);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("카운트가 임계치를 초과해도 true를 반환한다")
    void 카운트_임계치_초과시_true_반환() {
        when(valueOperations.increment(EXPECTED_KEY)).thenReturn(5L);

        boolean result = patternDetectService.incrementAndCheckThreshold(SERVICE_NAME, ERROR_TYPE);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("카운트가 임계치 미만이면 false를 반환한다")
    void 카운트_임계치_미달시_false_반환() {
        when(valueOperations.increment(EXPECTED_KEY)).thenReturn(2L);

        boolean result = patternDetectService.incrementAndCheckThreshold(SERVICE_NAME, ERROR_TYPE);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Redis에 값이 존재하면 해당 카운트를 반환한다")
    void getErrorCount_값_존재시_반환() {
        when(valueOperations.get(EXPECTED_KEY)).thenReturn("5");

        long count = patternDetectService.getErrorCount(SERVICE_NAME, ERROR_TYPE);

        assertThat(count).isEqualTo(5L);
    }

    @Test
    @DisplayName("Redis에 값이 없으면 0을 반환한다")
    void getErrorCount_값_없으면_0_반환() {
        when(valueOperations.get(EXPECTED_KEY)).thenReturn(null);

        long count = patternDetectService.getErrorCount(SERVICE_NAME, ERROR_TYPE);

        assertThat(count).isZero();
    }

    @Test
    @DisplayName("resetCount 호출 시 Redis 키를 삭제한다")
    void resetCount_Redis_키_삭제() {
        patternDetectService.resetCount(SERVICE_NAME, ERROR_TYPE);

        verify(redisTemplate).delete(EXPECTED_KEY);
    }
}
