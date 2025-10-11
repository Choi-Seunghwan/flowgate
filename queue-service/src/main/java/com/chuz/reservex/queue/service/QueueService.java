package com.chuz.reservex.queue.service;

import java.time.Instant;
import java.util.Random;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.chuz.reservex.queue.dto.EnqueueRes;
import com.chuz.reservex.queue.dto.StatusRes;

import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QueueService {
  private final StringRedisTemplate redis;
  private final Random random = new Random();

  @Value("${app.queue.passTokenTtlSeconds}")
  long passTtlSec;
  @Value("${app.queue.permitsPerMinute}")
  long permitsPerMinute;

  private String zKey(Long eventId) {
  return "q:%d:z".formatted(eventId);
  }

  private String sKey(Long eventId, Long userId) {
  return "q:%d:s:%d".formatted(eventId, userId);
  }

  private String passKey(Long eventId, Long userId) {
  return "q:%d:pass:%d".formatted(eventId, userId);
  }

  private String offsetKey(Long eventId, Long userId) {
  return "q:%d:offset:%d".formatted(eventId, userId);
  }

  public EnqueueRes enqueue(Long eventId, Long userId) {
  String userKey = userId.toString();
  String z = zKey(eventId);
  String s = sKey(eventId, userId);
  String offsetK = offsetKey(eventId, userId);

  if (Boolean.TRUE.equals(redis.hasKey(s))) {
    long pos = position(eventId, userKey);
    int offset = readOrGenOffset(offsetK);
    return new EnqueueRes(userKey, pos, offset);
  }

  long now = Instant.now().toEpochMilli();

  redis.opsForZSet().add(z, userKey, now);

  redis.opsForValue().set(s, "1");
  redis.expire(s, java.time.Duration.ofMinutes(30));

  int offset = 30 + random.nextInt(31);

  redis.opsForValue().set(offsetK, Integer.toString(offset));
  redis.expire(offsetK, java.time.Duration.ofMinutes(30));

  long pos = position(eventId, userKey);

  return new EnqueueRes(userKey, pos, offset);
  }

  public StatusRes status(Long eventId, Long userId) {
  String userKey = userId.toString();

  String pkey = passKey(eventId, userId);
  String pass = redis.opsForValue().get(pkey);

  if (pass != null) {
    return new StatusRes(0, true, pass);
  }

  long pos = position(eventId, userKey);

  long epochSec = Instant.now().getEpochSecond();
  long allowedSoFar = epochSec * Math.max(1, permitsPerMinute) / 60;

  if (pos == 0 && allowedSoFar >= grantedCountSoFar(eventId)) {
    String passToken = "pass_" + UUID.randomUUID();

    redis.opsForValue().set(pkey, passToken);
    redis.expire(pkey, java.time.Duration.ofSeconds(passTtlSec));

    redis.opsForZSet().remove(zKey(eventId), userKey);

    return new StatusRes(0, true, passToken);

  }

  return new StatusRes(pos, false, null);
  }

  private long position(Long eventId, String userKey) {
  Double score = redis.opsForZSet().score(zKey(eventId), userKey);

  if (score == null)
    return -1;

  Long rank = redis.opsForZSet().rank(zKey(eventId), userKey);
  return rank == null ? -1 : rank;
  }

  private int readOrGenOffset(String offsetK) {
  String s = redis.opsForValue().get(offsetK);
  return s == null ? 45 : Integer.parseInt(s);
  }

  private long grantedCountSoFar(Long eventId) {
  return 0L;
  }

  /**
   * Pass Token 검증 및 소비 (ticket-service에서 호출)
   */
  public boolean validateAndConsumePassToken(Long eventId, Long userId, String passToken) {
    String pkey = passKey(eventId, userId);

    // Redis에서 저장된 Pass Token 조회
    String storedToken = redis.opsForValue().get(pkey);

    if (storedToken == null || !storedToken.equals(passToken)) {
      return false;
    }

    // 검증 성공 시 토큰 삭제 (일회성 처리)
    redis.delete(pkey);

    return true;
  }
}
