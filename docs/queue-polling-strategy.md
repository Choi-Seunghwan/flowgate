# 대기열 폴링 전략 (Queue Polling Strategy)

## 개요

ReserveX는 **Short Polling + Adaptive Interval** 방식을 채택하여 대기열 상태를 조회함.

## 폴링 방식 선택 배경

### Short Polling vs Long Polling

| 방식              | 설명                              | 장점                                               | 단점                                                              |
| ----------------- | --------------------------------- | -------------------------------------------------- | ----------------------------------------------------------------- |
| **Short Polling** | 클라이언트가 주기적으로 상태 조회 | • 구현 간단<br>• Stateless 서버<br>• 스케일링 용이 | • 불필요한 요청 발생<br>• 최대 N초 지연                           |
| **Long Polling**  | 서버가 변경 시까지 대기 후 응답   | • 실시간성 향상<br>• 요청 수 감소                  | • Connection 유지 필요<br>• 스케일링 복잡<br>• MSA 구조와 안 맞음 |
| **WebSocket**     | 양방향 실시간 통신                | • 완전한 실시간<br>• 서버 푸시 가능                | • 연결 관리 복잡<br>• 대규모 접속 시 부담<br>• CDN/LB 호환성      |

### 선택: Short Polling

**이유:**

1. ✅ MSA 아키텍처와 Stateless 서버에 적합
2. ✅ Redis만으로 충분한 성능 (조회 성능 우수)
3. ✅ 구현 및 유지보수 단순
4. ✅ 로드밸런서, CDN과의 호환성
5. ✅ 수평 확장 용이

**실제 사례:**

- 멜론티켓, YES24, 알라딘 등 대부분의 티켓팅 서비스가 Short Polling 사용

## Adaptive Interval (적응형 폴링 간격)

대기 순번에 따라 폴링 간격을 조정하여 **불필요한 요청을 줄이고 실시간성을 확보**합니다.

### 폴링 간격 정책

| 대기 순번     | 폴링 간격 | 이유                                 |
| ------------- | --------- | ------------------------------------ |
| 1000+         | 30초      | 통과까지 오래 걸리므로 여유롭게 대기 |
| 100-999       | 10초      | 중간 단계, 적당한 간격 유지          |
| 10-99         | 5초       | 곧 차례이므로 자주 확인              |
| 1-9           | 2초       | 바로 차례, 빠른 응답 필요            |
| 0 (통과 가능) | -         | Pass Token 발급, 폴링 중단           |

### 구현 예시 (Client)

```javascript
// JavaScript/TypeScript
async function pollQueueStatus(eventId, jwtToken) {
  while (true) {
    const response = await fetch(`/queue/${eventId}/status`, {
      headers: { Authorization: `Bearer ${jwtToken}` },
    });

    const data = await response.json();

    // Pass Token 발급되면 폴링 종료
    if (data.canProceed) {
      console.log("✅ 통과! Pass Token:", data.passToken);
      return data.passToken;
    }

    // Adaptive Interval 계산
    const interval = calculatePollInterval(data.position);
    console.log(`대기 순번: ${data.position}, 다음 조회: ${interval}초 후`);

    await sleep(interval * 1000);
  }
}

function calculatePollInterval(position) {
  if (position >= 1000) return 30;
  if (position >= 100) return 10;
  if (position >= 10) return 5;
  if (position >= 1) return 2;
  return 1; // fallback
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
```

```kotlin
// Kotlin (Android)
suspend fun pollQueueStatus(eventId: Long, jwtToken: String): String {
    while (true) {
        val response = apiService.getQueueStatus(eventId, "Bearer $jwtToken")

        if (response.canProceed) {
            Log.d("Queue", "✅ 통과! Pass Token: ${response.passToken}")
            return response.passToken!!
        }

        val interval = calculatePollInterval(response.position)
        Log.d("Queue", "대기 순번: ${response.position}, 다음 조회: ${interval}초 후")

        delay(interval * 1000L)
    }
}

fun calculatePollInterval(position: Long): Int {
    return when {
        position >= 1000 -> 30
        position >= 100 -> 10
        position >= 10 -> 5
        position >= 1 -> 2
        else -> 1
    }
}
```

```swift
// Swift (iOS)
func pollQueueStatus(eventId: Int, jwtToken: String) async throws -> String {
    while true {
        let response = try await getQueueStatus(eventId: eventId, token: jwtToken)

        if response.canProceed {
            print("✅ 통과! Pass Token: \(response.passToken)")
            return response.passToken
        }

        let interval = calculatePollInterval(position: response.position)
        print("대기 순번: \(response.position), 다음 조회: \(interval)초 후")

        try await Task.sleep(nanoseconds: UInt64(interval * 1_000_000_000))
    }
}

func calculatePollInterval(position: Int) -> Int {
    switch position {
    case 1000...: return 30
    case 100..<1000: return 10
    case 10..<100: return 5
    case 1..<10: return 2
    default: return 1
    }
}
```

## API 명세

### 대기열 상태 조회

**Endpoint:** `GET /queue/{eventId}/status`

**Headers:**

```
Authorization: Bearer {JWT_TOKEN}
```

**Response:**

```json
{
  "position": 42,
  "canProceed": false,
  "passToken": null
}
```

**통과 가능 시:**

```json
{
  "position": 0,
  "canProceed": true,
  "passToken": "pass_abc123..."
}
```

## 클라이언트 구현 가이드

### 1. 폴링 시작 조건

- 사용자가 대기열 진입(`POST /queue/{eventId}/enqueue`) 성공 후
- JWT 토큰이 유효한 상태

### 2. 폴링 중단 조건

- `canProceed: true` 및 `passToken` 수신
- 사용자가 페이지 이탈
- 네트워크 에러 (재시도 로직 필요)

### 3. UI/UX 권장사항

```
┌─────────────────────────────┐
│  대기 중...                  │
│                              │
│  현재 대기 순번: 42명        │
│  예상 대기 시간: 약 7분      │
│                              │
│  [로딩 애니메이션]           │
│                              │
│  다음 업데이트: 5초 후       │
└─────────────────────────────┘
```

### 4. 에러 처리

```javascript
async function pollWithRetry(eventId, jwtToken, maxRetries = 3) {
  let retryCount = 0;

  while (true) {
    try {
      const response = await fetch(`/queue/${eventId}/status`, {
        headers: { Authorization: `Bearer ${jwtToken}` },
      });

      if (response.status === 401 || response.status === 403) {
        throw new Error("인증 만료. 다시 로그인해주세요.");
      }

      const data = await response.json();
      retryCount = 0; // 성공 시 재시도 카운트 리셋

      if (data.canProceed) {
        return data.passToken;
      }

      const interval = calculatePollInterval(data.position);
      await sleep(interval * 1000);
    } catch (error) {
      retryCount++;

      if (retryCount >= maxRetries) {
        throw new Error("대기열 조회 실패. 새로고침 후 다시 시도해주세요.");
      }

      console.warn(`재시도 ${retryCount}/${maxRetries}...`);
      await sleep(5000); // 5초 후 재시도
    }
  }
}
```

## 성능 분석

### 요청 수 비교

**시나리오:** 1000명 대기, 분당 100명 통과

#### 고정 간격 (3초)

```
1000명 × 20회/분 = 20,000 요청/분
```

#### Adaptive Interval

```
순번 1000-901 (1분): 100명 × 2회/분 = 200 요청
순번 900-101 (8분): 800명 × 6회/분 = 4,800 요청
순번 100-1 (1분): 100명 × 12회/분 = 1,200 요청

총: 6,200 요청/10분 = 620 요청/분
```

**결과:** 약 **97% 요청 감소** (20,000 → 620)

## Ghost User 처리

대기열에 있지만 실제로 이탈한 사용자를 처리하는 전략:

### 1. TTL 기반 자동 만료

```java
// 대기열 진입 시
redis.zadd(queueKey, score, userId);
redis.expire(userStateKey, Duration.ofMinutes(30));

// 상태 조회 시마다 TTL 갱신
redis.expire(userStateKey, Duration.ofMinutes(30));
```

### 2. Heartbeat 방식

- 클라이언트가 폴링할 때마다 서버가 "활성 상태"로 기록
- 5분간 조회 없으면 대기열에서 자동 제거

### 3. 구현 (Scheduled Task)

```java
@Scheduled(fixedRate = 60000) // 1분마다 실행
public void cleanupInactiveUsers() {
    // Redis에서 5분간 활동 없는 사용자 제거
    long cutoffTime = Instant.now().minus(5, ChronoUnit.MINUTES).toEpochMilli();
    redis.zremrangeByScore(queueKey, 0, cutoffTime);
}
```

## 모니터링 지표

운영 시 추적해야 할 지표:

1. **평균 폴링 간격:** 적응형 로직이 잘 작동하는지 확인
2. **요청 실패율:** 네트워크 안정성 확인
3. **Ghost User 비율:** 이탈률 추적
4. **평균 대기 시간:** 사용자 경험 지표

## 참고 자료

- [AWS - API Polling Best Practices](https://docs.aws.amazon.com/general/latest/gr/api-retries.html)
- [Redis Sorted Set Commands](https://redis.io/commands/?group=sorted-set)
- [인터파크 티켓 대기열 시스템 사례](https://tech.kakao.com/2020/12/15/live-commerce-server-architecture/)
