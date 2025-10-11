# ReserveX 문서 폴더

이 폴더에는 ReserveX 프로젝트의 모든 문서가 포함되어 있습니다.

## 📚 문서 목록

### [ARCHITECTURE.md](./ARCHITECTURE.md)
- 시스템 아키텍처 다이어그램
- 서비스별 역할 및 책임
- 데이터 모델 및 Redis 구조
- SAGA 패턴 설명
- 기술 스택

### [API-TEST-COMMANDS.md](./API-TEST-COMMANDS.md)
- 모든 API 엔드포인트 테스트 명령어
- curl 예시 및 응답 예시
- 전체 테스트 시나리오
- Redis/PostgreSQL 확인 명령어

### [test-queue.sh](./test-queue.sh)
- 대기열 자동 테스트 스크립트
- 대기열 진입 → 폴링 → Pass Token 발급까지 자동화
- 컬러 출력으로 가독성 향상

---

## 🚀 빠른 시작

### 1. 환경 실행
```bash
# Docker Compose 실행 (PostgreSQL, Redis, Kafka 등)
docker-compose up -d

# 서비스 실행
./gradlew :account-service:bootRun   # Terminal 1 (Port 8081)
./gradlew :queue-service:bootRun     # Terminal 2 (Port 8083)
./gradlew :ticket-service:bootRun    # Terminal 3 (Port 8080)
./gradlew :payment-service:bootRun   # Terminal 4 (Port 8082)
```

### 2. 대기열 테스트
```bash
# 자동 테스트
./docs/test-queue.sh

# 또는 수동 테스트
curl -X POST "http://localhost:8083/queue/1/enqueue?clientId=alice"
curl -X GET "http://localhost:8083/queue/1/status?clientId=alice"
```

### 3. 전체 예약 흐름 테스트
자세한 내용은 [API-TEST-COMMANDS.md](./API-TEST-COMMANDS.md) 참조

---

## 📖 추가 문서

프로젝트 루트의 [CLAUDE.md](../CLAUDE.md)에서 프로젝트 개요를 확인할 수 있습니다.
