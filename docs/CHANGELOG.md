# Changelog

ReserveX 프로젝트의 주요 변경사항을 기록합니다.

## 2025-10-12

### 🔐 JWT 인증 통합 (대기열 시스템)

**배경:**

- 기존: 임의의 `clientId`(문자열)로 대기열 진입 가능 → 인증 없음
- 문제: 누구나 대기열에 진입 가능, 보안 취약

**변경사항:**

#### 1. Queue Service 인증 추가

- **파일:** `queue-service/build.gradle`
  - Spring Security, JWT 의존성 추가
- **파일:** `queue-service/src/main/resources/application.yml`
  - JWT secret 설정 추가
- **파일:** `queue-service/src/main/java/com/chuz/reservex/queue/config/SecurityConfig.java` (신규)
  - JWT 필터 적용
  - `/actuator/health`, `/queue/*/validate-pass-token` 제외하고 인증 필수

#### 2. API 변경

- **파일:** `queue-service/src/main/java/com/chuz/reservex/queue/controller/QueueController.java`

  - Before: `@RequestParam String clientId`
  - After: `Authentication authentication` → JWT에서 `userId` 추출

- **파일:** `queue-service/src/main/java/com/chuz/reservex/queue/service/QueueService.java`
  - `clientId: String` → `userId: Long`으로 전면 변경
  - Redis 키 구조 변경: `u:{clientId}` → `{userId}`

#### 3. 인증 플로우

```
사용자 회원가입 (account-service)
    ↓
로그인 → JWT 토큰 발급
    ↓
대기열 진입 (JWT 헤더 포함)
    ↓
queue-service에서 JWT 검증 후 userId 추출
    ↓
대기열 상태 폴링 (JWT 헤더 포함)
    ↓
Pass Token 발급
    ↓
티켓 예매
```

---

### 🧪 E2E 테스트 환경 구축

**기술 스택:**

- REST Assured 5.5.0
- TestContainers 1.19.8 (PostgreSQL, Redis)
- Awaitility 4.2.0 (비동기 폴링 테스트)
- JUnit 5

**구조:**

```
e2e-tests/
├── build.gradle                           # 의존성 설정
├── README.md                              # 사용 가이드
└── src/test/java/com/chuz/reservex/e2e/
    ├── BaseE2ETest.java                   # TestContainers 설정
    └── QueueE2ETest.java                  # 대기열 E2E 테스트
```

**테스트 시나리오 (QueueE2ETest):**

1. ✅ 사용자 회원가입
2. ✅ 로그인하여 JWT 토큰 발급
3. ✅ JWT 토큰으로 대기열 진입
4. ✅ 대기열 상태 확인 및 Pass Token 발급 대기 (Awaitility 폴링)
5. ✅ 중복 대기열 진입 시 기존 위치 반환
6. ✅ 인증 없이 대기열 진입 시도 (실패 케이스)

**실행 방법:**

```bash
# E2E 테스트만 실행
./gradlew :e2e-tests:e2eTest

# 전체 테스트 실행
./gradlew :e2e-tests:test
```

**특징:**

- TestContainers로 실제 PostgreSQL, Redis 사용 (격리된 환경)
- REST Assured로 BDD 스타일 API 테스트
- Awaitility로 비동기 폴링 테스트 (최대 30초 대기)
- `@Order`로 테스트 순서 보장

---

### 📚 대기열 폴링 전략 문서화

**파일:** `docs/queue-polling-strategy.md`

**핵심 결정사항:**

- **방식:** Short Polling + Adaptive Interval 채택
- **이유:**
  - MSA/Stateless 아키텍처와 궁합
  - Redis만으로 충분한 성능
  - 스케일링 용이
  - Long Polling/WebSocket 대비 구현 단순

**Adaptive Interval 정책:**
| 대기 순번 | 폴링 간격 |
|----------|----------|
| 1000+ | 30초 |
| 100-999 | 10초 |
| 10-99 | 5초 |
| 1-9 | 2초 |

**성능 개선:**

- 고정 3초 폴링: 20,000 요청/분
- Adaptive Interval: 620 요청/분
- **97% 요청 감소**

**클라이언트 구현 예시 포함:**

- JavaScript/TypeScript
- Kotlin (Android)
- Swift (iOS)

**Ghost User 처리 전략:**

- TTL 기반 자동 만료 (30분)
- Heartbeat 방식 (5분 비활성 시 제거)
- Scheduled Task 구현 가이드

---

### 🔄 테스트 스크립트 업데이트

**파일:** `docs/test-queue.sh`

**변경사항:**

- Before: `clientId` 파라미터로 직접 대기열 진입
- After:
  1. account-service 로그인
  2. JWT 토큰 발급
  3. JWT 헤더와 함께 대기열 진입
  4. JWT 헤더와 함께 상태 폴링

**주의:**

- 실제 프로덕션 테스트는 E2E 테스트 모듈 사용 권장
- 쉘 스크립트는 빠른 수동 검증용

---

## 파일 변경 요약

### 신규 파일

- `queue-service/src/main/java/com/chuz/reservex/queue/config/SecurityConfig.java`
- `e2e-tests/build.gradle`
- `e2e-tests/src/test/java/com/chuz/reservex/e2e/BaseE2ETest.java`
- `e2e-tests/src/test/java/com/chuz/reservex/e2e/QueueE2ETest.java`
- `e2e-tests/src/test/resources/application-test.yml`
- `e2e-tests/README.md`
- `docs/queue-polling-strategy.md`
- `docs/CHANGELOG.md` (이 파일)

### 수정 파일

- `build.gradle` - 공통 테스트 의존성 추가
- `settings.gradle` - e2e-tests 모듈 추가
- `queue-service/build.gradle` - Spring Security, JWT 추가
- `queue-service/src/main/resources/application.yml` - JWT secret 추가
- `queue-service/src/main/java/com/chuz/reservex/queue/controller/QueueController.java`
- `queue-service/src/main/java/com/chuz/reservex/queue/service/QueueService.java`
- `docs/test-queue.sh` - JWT 인증 플로우 반영

---

## 다음 단계 (TODO)

### 높은 우선순위

- [ ] Ghost User Cleanup Scheduler 구현
- [ ] Adaptive Polling Interval 서버 최적화
- [ ] Pass Token 검증 로직 ticket-service 연동

### 중간 우선순위

- [ ] 대기열 통과 후 자동 제거 로직
- [ ] 모니터링 지표 추가 (Prometheus/Grafana)
- [ ] 부하 테스트 (K6/Gatling)

### 낮은 우선순위

- [ ] 대기 예상 시간 계산 로직
- [ ] 관리자 대시보드 (대기열 현황)
- [ ] 우선순위 큐 (VIP 사용자)

---

## 기술 부채

1. **JWT Secret 관리**

   - 현재: application.yml에 하드코딩
   - 개선: 환경변수 또는 AWS Secrets Manager

2. **서비스 간 통신 인증**

   - 현재: validate-pass-token은 인증 없음
   - 개선: Service-to-Service JWT 또는 API Key

3. **Redis 단일 장애점**
   - 현재: Redis 한 대
   - 개선: Redis Sentinel 또는 Cluster

---

## 참고 링크

- [Common 모듈 JWT 구현](../common/src/main/java/com/chuz/reservex/common/security/)
- [E2E 테스트 가이드](../e2e-tests/README.md)
- [폴링 전략 상세](./queue-polling-strategy.md)
