# ReserveX

**확장 가능한 티켓 예매 시스템** - MSA 아키텍처와 SAGA 패턴 구현

## 프로젝트 개요

ReserveX 대규모 트래픽을 처리를 위한 티켓 예매 시스템

### 주요 기능

- ✅ **대기열 시스템**: Redis 기반 실시간 대기열 관리
- ✅ **분산 트랜잭션**: SAGA 패턴을 통한 예매-결제 처리
- ✅ **MSA 아키텍처**: 독립적으로 배포 가능한 마이크로서비스
- ✅ **이벤트 기반 통신**: Kafka를 통한 서비스 간 비동기 통신

### 기술 스택

| 카테고리          | 기술                       |
| ----------------- | -------------------------- |
| **Backend**       | Java 21, Spring Boot 3.5.4 |
| **Database**      | PostgreSQL 14              |
| **Message Queue** | Apache Kafka               |
| **Cache**         | Redis 7                    |
| **Security**      | Spring Security, JWT       |
| **Build Tool**    | Gradle                     |

## 빠른 시작

### 1. 사전 요구사항

- **Docker & Docker Compose** (필수)
- **Java 21** (서비스 실행 시)
- **Gradle**

### 2. 인프라 실행

```bash
# 1. Docker Compose로 인프라 실행 (PostgreSQL, Kafka, Redis, Zookeeper)
docker-compose up -d

# 2. 인프라 상태 확인
docker-compose ps

# 3. 로그 확인
docker-compose logs -f
```

실행되는 컨테이너:

- ✅ PostgreSQL (port 5432) - 3개 데이터베이스 자동 생성
- ✅ Kafka (port 9092)
- ✅ Zookeeper (port 2181)
- ✅ Redis (port 6379)

### 3. 서비스 실행

각 서비스를 별도 터미널에서 실행:

```bash
# Terminal 1: Account Service (포트 8081)
./gradlew :account-service:bootRun

# Terminal 2: Ticket Service (포트 8080)
./gradlew :ticket-service:bootRun

# Terminal 3: Payment Service (포트 8082)
./gradlew :payment-service:bootRun
```

### 4. 동작 확인

```bash
# Health Check
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

## SAGA 패턴 흐름

자세한 내용은 [SAGA_PATTERN.md](SAGA_PATTERN.md) 참조

### 정상 흐름

```
사용자 → 예매 요청 → 재고 감소 → 결제 요청(Kafka)
                               ↓
                          결제 완료(Kafka)
                               ↓
                          예매 확정 ✅
```

### 실패 흐름 (보상 트랜잭션)

```
사용자 → 예매 요청 → 재고 감소 → 결제 요청(Kafka)
                               ↓
                          결제 실패(Kafka)
                               ↓
                    재고 복구 + 예매 취소 ❌
```

## Kafka 메시지 모니터링

```bash
# 예약 생성 이벤트
docker exec -it reservex-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic saga.reservation.created \
  --from-beginning

# 결제 완료 이벤트
docker exec -it reservex-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic saga.payment.completed \
  --from-beginning
```

## 인프라 종료

```bash
# 서비스 중지
docker-compose stop

# 서비스 중지 및 컨테이너 삭제
docker-compose down

# 데이터까지 모두 삭제
docker-compose down -v
```

## 트러블슈팅

### 포트 충돌

```bash
# 포트 사용 확인
lsof -i :8080
lsof -i :5432

# 프로세스 종료
kill -9 <PID>
```

### Kafka 연결 실패

```bash
# Kafka 재시작
docker-compose restart kafka zookeeper
```
