# ReserveX 아키텍처 문서

## 프로젝트 개요

**ReserveX** - 대기열 시스템과 SAGA 패턴을 적용한 MSA 기반 티켓 예매 플랫폼

---

## 시스템 아키텍처

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │
       ├─────────────────────────────────────────┐
       │                                         │
       ▼                                         ▼
┌─────────────────┐                    ┌──────────────────┐
│ account-service │                    │  queue-service   │
│   (Port 8081)   │                    │   (Port 8083)    │
│                 │                    │                  │
│  - 회원가입      │                    │  - 대기열 관리    │
│  - 로그인        │                    │  - Pass Token    │
│  - JWT 발급     │                    │  - Rate Limiting │
└────────┬────────┘                    └────────┬─────────┘
         │                                      │
         │         PostgreSQL (5433)            │    Redis (6380)
         ├──────────────┬──────────────────────┼────────────┐
         │              │                      │            │
         │              ▼                      ▼            │
         │      ┌──────────────────┐    ┌──────────────┐   │
         │      │  ticket-service  │    │  Sorted Set  │   │
         │      │   (Port 8080)    │    │  q:{id}:z    │   │
         │      │                  │    │              │   │
         │      │  - 예약 생성      │    │  Pass Token  │   │
         │      │  - 재고 관리      │    │  q:{id}:pass │   │
         │      │  - SAGA 오케스트레이션│ └──────────────┘   │
         │      └────────┬─────────┘                        │
         │               │                                  │
         │               │  Kafka (9093)                    │
         │               ├───────────────────┐              │
         │               │                   │              │
         │               ▼                   ▼              │
         │      ┌──────────────────┐  ┌──────────────┐     │
         │      │ payment-service  │  │   Zookeeper  │     │
         │      │   (Port 8082)    │  │  (Port 2182) │     │
         │      │                  │  └──────────────┘     │
         │      │  - 결제 처리      │                        │
         │      │  - SAGA 이벤트   │                        │
         │      └──────────────────┘                        │
         │                                                  │
         └──────────────────────────────────────────────────┘
                          JWT Token
```

---

## 서비스별 역할

### 1. **account-service** (인증 서비스)
- **포트**: 8081
- **데이터베이스**: PostgreSQL (reservex_account)
- **책임**:
  - 사용자 회원가입/로그인
  - JWT Access/Refresh Token 발급
  - 사용자 인증

### 2. **queue-service** (대기열 서비스)
- **포트**: 8083
- **저장소**: Redis
- **책임**:
  - 이벤트별 대기열 관리 (Sorted Set)
  - Pass Token 발급 및 검증
  - Rate Limiting (분당 통과 인원 제어)
  - 대기 순번 계산

### 3. **ticket-service** (예약 서비스)
- **포트**: 8080
- **데이터베이스**: PostgreSQL (reservex_ticket)
- **책임**:
  - 이벤트 및 상품(티켓) 관리
  - 예약 생성 및 관리
  - 재고 관리
  - SAGA 오케스트레이션 (예약 ↔ 결제)

### 4. **payment-service** (결제 서비스)
- **포트**: 8082
- **데이터베이스**: PostgreSQL (reservex_payment)
- **책임**:
  - 결제 처리
  - SAGA 이벤트 처리
  - 보상 트랜잭션 (결제 실패 시)

### 5. **common** (공통 모듈)
- JWT 인증/인가 (JwtTokenProvider, JwtAuthenticationFilter)
- SAGA 이벤트 정의
- 전역 예외 처리

---

## 데이터 모델

### Event (이벤트)
```
events
├─ id: Long (PK)
├─ name: String (이벤트명)
├─ description: String
├─ event_date: LocalDateTime
├─ venue: String (장소)
├─ sale_start_at: LocalDateTime
├─ sale_end_at: LocalDateTime
├─ status: EventStatus (SCHEDULED, SELLING, SOLD_OUT, ENDED, CANCELLED)
└─ created_at, updated_at: LocalDateTime
```

### Product (상품 - 티켓 타입)
```
products
├─ id: Long (PK)
├─ event_id: Long (FK → events)
├─ name: String (VIP석, 일반석 등)
├─ description: String
├─ price: BigDecimal
├─ total_stock: Integer
├─ available_stock: Integer
├─ sale_start_at: LocalDateTime
├─ sale_end_at: LocalDateTime
└─ created_at, updated_at: LocalDateTime
```

### Reservation (예약)
```
reservations
├─ id: Long (PK)
├─ user_id: Long (account-service User ID)
├─ product_id: Long (FK → products)
├─ quantity: Integer
├─ total_price: BigDecimal
├─ status: ReservationStatus (PENDING, PAYMENT_PENDING, CONFIRMED, CANCELLED)
├─ saga_id: String (SAGA 트랜잭션 ID)
└─ created_at, updated_at: LocalDateTime
```

### User (사용자)
```
users (account-service)
├─ id: Long (PK)
├─ email: String (UNIQUE)
├─ password: String (암호화)
├─ name: String
├─ phone_number: String
├─ role: UserRole (USER, ADMIN)
└─ created_at, updated_at: LocalDateTime
```

---

## Redis 데이터 구조

### 대기열 (Sorted Set)
```
Key: q:{eventId}:z
Type: Sorted Set
Score: timestamp (enqueue 시간)
Member: u:{clientId}

예시:
q:1:z
  u:alice   -> 1728456780000
  u:bob     -> 1728456785000
  u:charlie -> 1728456790000
```

### Pass Token (String)
```
Key: q:{eventId}:pass:u:{clientId}
Type: String
Value: pass_{UUID}
TTL: 300초 (5분)

예시:
q:1:pass:u:alice -> "pass_abc-123-def-456"
```

### 대기 상태 (String)
```
Key: q:{eventId}:s:u:{clientId}
Type: String
Value: "1"
TTL: 1800초 (30분)
```

---

## SAGA 패턴 (Choreography)

### 성공 시나리오
```
[ticket-service]
   ① 예약 생성 (PAYMENT_PENDING)
   ② 재고 감소
   ③ ReservationCreatedEvent 발행

         ↓ Kafka

[payment-service]
   ④ 결제 처리
   ⑤ PaymentCompletedEvent 발행

         ↓ Kafka

[ticket-service]
   ⑥ 예약 확정 (CONFIRMED)
```

### 실패 시나리오 (보상 트랜잭션)
```
[ticket-service]
   ① 예약 생성 (PAYMENT_PENDING)
   ② 재고 감소
   ③ ReservationCreatedEvent 발행

         ↓ Kafka

[payment-service]
   ④ 결제 실패
   ⑤ PaymentFailedEvent 발행

         ↓ Kafka

[ticket-service]
   ⑥ 재고 복구 (보상)
   ⑦ 예약 취소 (CANCELLED)
   ⑧ ReservationCancelledEvent 발행
```

---

## 전체 예약 흐름

```
1. 사용자 인증
   POST /api/auth/login
   → JWT Token 발급

2. 대기열 진입
   POST /queue/{eventId}/enqueue?clientId={userId}
   → 대기 순번 부여

3. 대기 상태 폴링
   GET /queue/{eventId}/status?clientId={userId}
   → Pass Token 발급 (순번이 되면)

4. 티켓 예약
   POST /api/reservations?eventId={eventId}&clientId={userId}
   Headers:
     - Authorization: Bearer {jwt-token}
     - X-Pass-Token: {pass-token}
   Body: { "productId": 1, "quantity": 2 }

   → Pass Token 검증 (queue-service API 호출)
   → 재고 감소
   → SAGA 시작 (ReservationCreatedEvent)

5. 결제 처리 (자동)
   payment-service가 이벤트 수신
   → 결제 처리
   → PaymentCompletedEvent 발행

6. 예약 확정 (자동)
   ticket-service가 이벤트 수신
   → 예약 상태 → CONFIRMED
```

---

## 기술 스택

### Backend
- Java 21
- Spring Boot 3.5.4
- Spring Data JPA
- Spring Security
- Spring Kafka

### Database
- PostgreSQL 14
- Redis 7

### Message Queue
- Apache Kafka 7.5.0
- Zookeeper

### Build Tool
- Gradle 9.0.0

### 인증
- JWT (jjwt 0.12.3)

---

## 주요 설정

### 포트
- account-service: 8081
- ticket-service: 8080
- payment-service: 8082
- queue-service: 8083
- PostgreSQL: 5433
- Redis: 6380
- Kafka: 9093
- Zookeeper: 2182

### 대기열 설정
- Pass Token TTL: 5분 (300초)
- 분당 통과 인원: 100명
- 랜덤 폴링 간격: 30~60초

### JWT 설정
- Access Token 만료: 24시간
- Refresh Token 만료: 7일

---

## MSA 원칙 준수

✅ **Database per Service** - 각 서비스가 독립적인 DB 소유
✅ **API-based Communication** - REST API 및 Kafka 이벤트
✅ **Domain Boundary** - 명확한 도메인 경계
✅ **Loose Coupling** - 서비스 간 느슨한 결합
✅ **Independent Deployment** - 독립적인 배포 가능
✅ **SAGA Pattern** - 분산 트랜잭션 관리
