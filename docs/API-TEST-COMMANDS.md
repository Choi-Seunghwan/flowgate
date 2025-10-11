# ReserveX API 테스트 명령어 모음

## 1. Queue Service (대기열) - Port 8083

### 1-1. 대기열 진입
```bash
curl -X POST "http://localhost:8083/queue/1/enqueue?clientId=alice"
```

**응답 예시:**
```json
{
  "userKey": "u:alice",
  "position": 0,
  "randomOffset": 45
}
```

### 1-2. 대기 상태 확인 (폴링)
```bash
curl -X GET "http://localhost:8083/queue/1/status?clientId=alice"
```

**응답 예시 (대기 중):**
```json
{
  "position": 42,
  "canProceed": false,
  "passToken": null
}
```

**응답 예시 (통과!):**
```json
{
  "position": 0,
  "canProceed": true,
  "passToken": "pass_abc-123-def-456"
}
```

### 1-3. Pass Token 검증 (내부 API)
```bash
curl -X POST "http://localhost:8083/queue/1/validate-pass-token?clientId=alice&passToken=pass_abc-123-def-456"
```

---

## 2. Account Service (인증) - Port 8081

### 2-1. 회원가입
```bash
curl -X POST http://localhost:8081/api/auth/signup \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "alice@example.com",
    "password": "password123",
    "name": "Alice",
    "phoneNumber": "010-1234-5678"
  }'
```

**응답:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "userId": 1,
  "email": "alice@example.com",
  "name": "Alice"
}
```

### 2-2. 로그인
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "alice@example.com",
    "password": "password123"
  }'
```

---

## 3. Ticket Service (예약) - Port 8080

### 3-1. 예약 생성 (Pass Token 필요)
```bash
curl -X POST "http://localhost:8080/api/reservations?eventId=1&clientId=alice" \
  -H 'Authorization: Bearer YOUR_JWT_TOKEN' \
  -H 'X-Pass-Token: pass_abc-123-def-456' \
  -H 'Content-Type: application/json' \
  -d '{
    "productId": 1,
    "quantity": 2
  }'
```

### 3-2. 내 예약 목록 조회
```bash
curl -X GET http://localhost:8080/api/reservations/my \
  -H 'Authorization: Bearer YOUR_JWT_TOKEN'
```

### 3-3. 예약 상세 조회
```bash
curl -X GET http://localhost:8080/api/reservations/1 \
  -H 'Authorization: Bearer YOUR_JWT_TOKEN'
```

---

## 4. 전체 테스트 시나리오

### Step 1: Docker Compose 실행
```bash
docker-compose up -d
```

### Step 2: 서비스 실행
```bash
# Terminal 1 - Queue Service
./gradlew :queue-service:bootRun

# Terminal 2 - Account Service
./gradlew :account-service:bootRun

# Terminal 3 - Ticket Service
./gradlew :ticket-service:bootRun

# Terminal 4 - Payment Service
./gradlew :payment-service:bootRun
```

### Step 3: 회원가입 & 로그인
```bash
# 회원가입
curl -X POST http://localhost:8081/api/auth/signup \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "alice@example.com",
    "password": "password123",
    "name": "Alice",
    "phoneNumber": "010-1234-5678"
  }'

# JWT 토큰 저장 (응답에서 accessToken 복사)
export JWT_TOKEN="eyJhbGciOiJIUzI1NiIs..."
```

### Step 4: 대기열 진입 및 통과
```bash
# 자동 테스트 스크립트 실행
./test-queue.sh

# 또는 수동 테스트
curl -X POST "http://localhost:8083/queue/1/enqueue?clientId=alice"
curl -X GET "http://localhost:8083/queue/1/status?clientId=alice"

# Pass Token 저장
export PASS_TOKEN="pass_abc-123-def-456"
```

### Step 5: 티켓 예약
```bash
curl -X POST "http://localhost:8080/api/reservations?eventId=1&clientId=alice" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "X-Pass-Token: $PASS_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "productId": 1,
    "quantity": 2
  }'
```

### Step 6: 예약 확인
```bash
curl -X GET http://localhost:8080/api/reservations/my \
  -H "Authorization: Bearer $JWT_TOKEN"
```

---

## 5. Redis 데이터 확인

### Redis CLI 접속
```bash
docker exec -it reservex-redis redis-cli
```

### 대기열 확인
```redis
# Event 1번의 대기열 확인
ZRANGE q:1:z 0 -1 WITHSCORES

# Pass Token 확인
KEYS q:1:pass:*
GET q:1:pass:u:alice
```

---

## 6. PostgreSQL 데이터 확인

### PostgreSQL 접속
```bash
docker exec -it reservex-postgres psql -U dev -d reservex_ticket
```

### 데이터 조회
```sql
-- Event 목록
SELECT * FROM events;

-- Product 목록
SELECT * FROM products WHERE event_id = 1;

-- 예약 목록
SELECT * FROM reservations;
```

---

## 참고사항

- **eventId**: 1 = "아이유 콘서트 2025", 2 = "BTS 월드투어"
- **productId**: 1 = VIP석, 2 = 일반석, 3 = 스탠딩
- **Pass Token 유효시간**: 5분 (300초)
- **대기열 통과 속도**: 분당 100명 (설정 변경 가능)
