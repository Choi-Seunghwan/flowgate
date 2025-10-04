# SAGA Pattern 구현 가이드

## 개요

이 프로젝트는 **Choreography 기반 SAGA 패턴**을 사용하여 MSA 환경에서 분산 트랜잭션을 처리합니다.

## 아키텍처

### 서비스 구성
- **ticket-service** (port 8080): 티켓 예매 관리
- **payment-service** (port 8082): 결제 처리
- **queue-service** (port 8081): 대기열 관리
- **account-service**: 계정 관리

### SAGA 흐름

```
┌─────────────────────────────────────────────────────────────────┐
│                     예매-결제 SAGA 패턴                           │
└─────────────────────────────────────────────────────────────────┘

1. 정상 흐름 (Success Flow)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

   Client
     │
     │ POST /reservations
     ▼
┌──────────────────┐
│ Ticket Service   │
├──────────────────┤
│ 1. 재고 감소     │
│ 2. 예약 생성     │
│ 3. 상태: PENDING │
└────────┬─────────┘
         │
         │ ReservationCreatedEvent
         │ (Kafka: saga.reservation.created)
         ▼
┌──────────────────┐
│ Payment Service  │
├──────────────────┤
│ 1. 결제 처리     │
│ 2. 상태: SUCCESS │
└────────┬─────────┘
         │
         │ PaymentCompletedEvent
         │ (Kafka: saga.payment.completed)
         ▼
┌──────────────────┐
│ Ticket Service   │
├──────────────────┤
│ 예약 확정        │
│ 상태: CONFIRMED  │
└──────────────────┘


2. 보상 흐름 (Compensation Flow)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

   Client
     │
     │ POST /reservations
     ▼
┌──────────────────┐
│ Ticket Service   │
├──────────────────┤
│ 1. 재고 감소     │
│ 2. 예약 생성     │
│ 3. 상태: PENDING │
└────────┬─────────┘
         │
         │ ReservationCreatedEvent
         │ (Kafka: saga.reservation.created)
         ▼
┌──────────────────┐
│ Payment Service  │
├──────────────────┤
│ 결제 실패 ❌     │
│ (잔액부족 등)    │
└────────┬─────────┘
         │
         │ PaymentFailedEvent
         │ (Kafka: saga.payment.failed)
         ▼
┌──────────────────┐
│ Ticket Service   │
├──────────────────┤
│ 1. 재고 복구 ↻   │
│ 2. 예약 취소     │
│ 3. 상태: CANCELLED│
└────────┬─────────┘
         │
         │ ReservationCancelledEvent
         │ (Kafka: saga.reservation.cancelled)
         ▼
     완료
```

## 주요 컴포넌트

### 1. Common 모듈 (공통 이벤트 정의)

**SAGA 이벤트**
- `SagaEvent`: 기본 이벤트 클래스
- `ReservationCreatedEvent`: 예약 생성 이벤트
- `PaymentCompletedEvent`: 결제 완료 이벤트
- `PaymentFailedEvent`: 결제 실패 이벤트
- `ReservationCancelledEvent`: 예약 취소 이벤트

**Kafka 토픽**
```java
public class SagaTopics {
    public static final String RESERVATION_CREATED = "saga.reservation.created";
    public static final String PAYMENT_COMPLETED = "saga.payment.completed";
    public static final String PAYMENT_FAILED = "saga.payment.failed";
    public static final String RESERVATION_CANCELLED = "saga.reservation.cancelled";
}
```

### 2. Ticket Service

**ReservationSagaService**
- `createReservationAndStartSaga()`: SAGA 시작
- `handlePaymentCompleted()`: 결제 완료 처리
- `handlePaymentFailed()`: 결제 실패 처리 (보상 트랜잭션)

**Reservation Entity 상태**
```java
public enum ReservationStatus {
    PENDING,          // 예약 생성됨, 결제 대기 중
    PAYMENT_PENDING,  // 결제 처리 중
    CONFIRMED,        // 결제 완료
    CANCELLED         // 취소
}
```

### 3. Payment Service

**PaymentService**
- `handleReservationCreated()`: 예약 생성 이벤트 수신 → 결제 처리
- `processPayment()`: 실제 결제 로직 (현재는 Mock)

**Payment Entity 상태**
```java
public enum PaymentStatus {
    PENDING,     // 결제 대기
    PROCESSING,  // 결제 처리 중
    COMPLETED,   // 결제 완료
    FAILED       // 결제 실패
}
```

## 실행 방법

### 1. Kafka 실행
```bash
docker-compose up -d
```

### 2. PostgreSQL 데이터베이스 생성
```sql
CREATE DATABASE flowgate_ticket;
CREATE DATABASE flowgate_payment;
```

### 3. 서비스 실행
```bash
# Ticket Service
./gradlew :ticket-service:bootRun

# Payment Service
./gradlew :payment-service:bootRun

# Queue Service (선택)
./gradlew :queue-service:bootRun
```

## API 예제

### 예약 생성 (SAGA 시작)
```bash
POST /api/reservations
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

{
  "userId": 1,
  "productId": 1,
  "quantity": 2
}
```

## 모니터링

### Kafka 메시지 확인
```bash
# 예약 생성 이벤트
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic saga.reservation.created \
  --from-beginning

# 결제 완료 이벤트
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic saga.payment.completed \
  --from-beginning
```

## SAGA 패턴 장단점

### ✅ 장점
1. **느슨한 결합**: 서비스 간 독립성 유지
2. **확장성**: 새로운 서비스 추가 용이
3. **장애 격리**: 한 서비스의 장애가 전체 시스템에 영향 미치지 않음

### ⚠️ 단점
1. **복잡성**: 이벤트 추적 및 디버깅 어려움
2. **일관성**: 최종 일관성(Eventual Consistency)만 보장
3. **테스트**: 분산 환경 테스트 복잡

## 향후 개선 사항

- [ ] SAGA Orchestrator 패턴으로 변경 검토
- [ ] 이벤트 재시도 및 Dead Letter Queue 추가
- [ ] 분산 트레이싱 (Zipkin, Jaeger) 도입
- [ ] 이벤트 소싱(Event Sourcing) 패턴 적용
