package com.chuz.flowgate.ticket.config;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Pass Token 검증기
 * queue-service에서 발급한 대기열 통과 토큰을 검증하고 일회성으로 삭제
 */
@Component
@RequiredArgsConstructor
public class PassTokenValidator {

    private final StringRedisTemplate redis;

    /**
     * Pass Token 검증 및 삭제 (일회성)
     *
     * @param eventId   이벤트 ID
     * @param clientId  클라이언트 ID
     * @param passToken Pass Token
     * @return 검증 성공 여부
     */
    public boolean validateAndConsume(Long eventId, String clientId, String passToken) {
        if (passToken == null || passToken.isBlank()) {
            return false;
        }

        String userKey = "u:" + clientId;
        String redisKey = "q:%d:pass:%s".formatted(eventId, userKey);

        // Redis에서 저장된 Pass Token 조회
        String storedToken = redis.opsForValue().get(redisKey);

        if (storedToken == null || !storedToken.equals(passToken)) {
            return false;
        }

        // 검증 성공 시 토큰 삭제 (일회성 처리)
        redis.delete(redisKey);

        return true;
    }
}
