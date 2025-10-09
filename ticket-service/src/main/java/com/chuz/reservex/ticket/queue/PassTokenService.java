package com.chuz.reservex.ticket.queue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Pass Token 서비스
 * queue-service와 통신하여 Pass Token 검증
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PassTokenService {

  private final QueueServiceClient queueServiceClient;

  /**
   * Pass Token 검증 및 소비 (일회성)
   * queue-service에 API 호출하여 검증
   */
  public boolean validateAndConsume(Long eventId, String clientId, String passToken) {
    if (passToken == null || passToken.isBlank()) {
      log.debug("Pass Token이 비어있음");
      return false;
    }

    // queue-service로 검증 요청
    boolean isValid = queueServiceClient.validatePassToken(eventId, clientId, passToken);

    if (isValid) {
      log.info("Pass Token 검증 및 소비 완료: eventId={}, clientId={}", eventId, clientId);
    } else {
      log.warn("Pass Token 검증 실패: eventId={}, clientId={}", eventId, clientId);
    }

    return isValid;
  }
}
