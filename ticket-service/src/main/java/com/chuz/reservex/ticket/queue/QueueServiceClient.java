package com.chuz.reservex.ticket.queue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Queue Service API 클라이언트
 * MSA 서비스 간 통신
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QueueServiceClient {

  private final RestTemplate restTemplate;

  @Value("${app.queue-service.url:http://localhost:8083}")
  private String queueServiceUrl;

  /**
   * Pass Token 검증 요청 (queue-service로 API 호출)
   */
  public boolean validatePassToken(Long eventId, String clientId, String passToken) {
    try {
      String url = String.format("%s/queue/%d/validate-pass-token?clientId=%s&passToken=%s",
          queueServiceUrl, eventId, clientId, passToken);

      Boolean result = restTemplate.postForObject(url, null, Boolean.class);

      log.debug("Queue Service 검증 결과: eventId={}, clientId={}, result={}",
          eventId, clientId, result);

      return Boolean.TRUE.equals(result);

    } catch (Exception e) {
      log.error("Queue Service 호출 실패: eventId={}, clientId={}", eventId, clientId, e);
      return false;
    }
  }
}
