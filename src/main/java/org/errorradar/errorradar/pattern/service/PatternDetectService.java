package org.errorradar.errorradar.pattern.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatternDetectService {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${alert.threshold.count}")
    private int thresholdCount;

    @Value("${alert.threshold.window-minutes}")
    private int windowMinutes;

    /**
     * 에러 카운트 증가 및 임계치 초과 여부 반환
     * Redis Key: error:count:{serviceName}:{errorType}
     * TTL: windowMinutes (30분)
     */
    public boolean incrementAndCheckThreshold(String serviceName, String errorType) {
        String key = buildKey(serviceName, errorType);

        // 카운트 증가
        Long count = redisTemplate.opsForValue().increment(key);

        // 처음 생성시 TTL 설정 (30분)
        if(count != null && count == 1) redisTemplate.expire(key, Duration.ofMinutes(windowMinutes));

        log.info("PatternDetect 에러 카운트 - key: {}, count: {}/{}", key, count, thresholdCount);

        // 임계치 초과 여부 반환
        return count != null && count >= thresholdCount;
    }

    // 현재 에러 카운트 조회
    public long getErrorCount(String serviceName, String errorType) {
        String key = buildKey(serviceName,errorType);
        String count = redisTemplate.opsForValue().get(key);
        return count != null ? Long.parseLong(count) : 0;
    }

    // 에러 카운트 초기화(중복알림 방지)
    public void resetCount(String serviceName, String errorType) {
        String key = buildKey(serviceName, errorType);
        redisTemplate.delete(key);
        log.info("PatternDetectService 에러 카운트 초기화 key: {}", key);
    }

    private String buildKey(String serviceName, String errorType) {
        return String.format("error:count:%s:%s", serviceName, errorType);
    }
}
