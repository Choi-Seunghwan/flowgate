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

  private String sKey(Long eventId, String userKey) {
  return "q:%d:s:%s".formatted(eventId, userKey);
  }

  private String passKey(Long eventId, String userKey) {
  return "q:%d:pass:%s".formatted(eventId, userKey);
  }

  private String offsetKey(Long eventId, String userKey) {
  return "q:%d:offset:%s".formatted(eventId, userKey);
  }

  private String userKey(String clientId) {
  return "u:%s".formatted(clientId);
  }

  public EnqueueRes enqueue(Long eventId, String clientId) {
  String userKey = userKey(clientId);
  String z = zKey(eventId);
  String s = sKey(eventId, userKey);
  String offsetK = offsetKey(eventId, userKey);

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

  public StatusRes status(Long eventId, String clientId) {
  String userKey = userKey(clientId);

  String pkey = passKey(eventId, userKey);
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
}
