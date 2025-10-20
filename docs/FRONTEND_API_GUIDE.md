# ReserveX Frontend API Guide

프론트엔드 개발자를 위한 백엔드 API 통합 가이드입니다.

## 목차
- [시스템 개요](#시스템-개요)
- [로컬 개발 환경](#로컬-개발-환경)
- [인증 시스템](#인증-시스템)
- [API 엔드포인트](#api-엔드포인트)
- [예매 플로우](#예매-플로우)
- [에러 처리](#에러-처리)

---

## 시스템 개요

ReserveX는 MSA 아키텍처로 구성된 티켓 예매 시스템입니다.

### 서비스 구성

| 서비스 | 포트 | 역할 |
|--------|------|------|
| **account-service** | 8081 | 회원가입, 로그인, 인증 |
| **ticket-service** | 8080 | 상품 조회, 예매 관리 |
| **queue-service** | 8083 | 대기열 관리 |
| **payment-service** | 8082 | 결제 처리 (내부 통신) |

### 아키텍처 특징

- **JWT 인증**: Bearer Token 방식
- **대기열 시스템**: Redis 기반 실시간 대기열
- **SAGA 패턴**: 예매 → 결제 → 티켓 발급 분산 트랜잭션

---

## 로컬 개발 환경

### 1. 백엔드 실행

```bash
# 인프라 실행 (Docker)
docker-compose up -d

# 각 서비스 실행 (별도 터미널)
cd /Users/csh/project/flowgate
./gradlew :account-service:bootRun
./gradlew :ticket-service:bootRun
./gradlew :queue-service:bootRun
```

### 2. Base URL

```javascript
const API_BASE_URLS = {
  auth: 'http://localhost:8081',
  ticket: 'http://localhost:8080',
  queue: 'http://localhost:8083',
};
```

### 3. CORS 설정

모든 서비스는 CORS가 활성화되어 있으며, 로컬 개발 시 `localhost:3000` (또는 프론트 포트)를 허용합니다.

---

## 인증 시스템

### JWT 토큰 구조

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": 1,
  "email": "user@example.com",
  "name": "홍길동"
}
```

### 토큰 저장 및 사용

```javascript
// 로그인 성공 시 저장
localStorage.setItem('accessToken', response.accessToken);
localStorage.setItem('refreshToken', response.refreshToken);

// API 호출 시 헤더에 포함
const headers = {
  'Authorization': `Bearer ${localStorage.getItem('accessToken')}`,
  'Content-Type': 'application/json',
};
```

### 토큰 만료 처리

- **Access Token**: 24시간 유효
- **Refresh Token**: 7일 유효

```javascript
// 401 에러 발생 시 토큰 갱신
if (response.status === 401) {
  const newTokens = await refreshAccessToken();
  // 재시도
}
```

---

## API 엔드포인트

### 1. 인증 (Account Service) - Port 8081

#### 회원가입
```http
POST http://localhost:8081/api/auth/signup
Content-Type: application/json

{
  "email": "user@example.com",
  "name": "홍길동",
  "password": "password123",
  "phoneNumber": "010-1234-5678"
}
```

**응답:**
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "userId": 1,
  "email": "user@example.com",
  "name": "홍길동"
}
```

#### 로그인
```http
POST http://localhost:8081/api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

**응답:** 회원가입과 동일

#### 토큰 갱신
```http
POST http://localhost:8081/api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJ..."
}
```

---

### 2. 상품 조회 (Ticket Service) - Port 8080

#### 전체 상품 목록 조회
```http
GET http://localhost:8080/api/products
Authorization: Bearer {accessToken}
```

**응답:**
```json
[
  {
    "id": 1,
    "name": "VIP석",
    "description": "무대 앞 최고의 좌석",
    "price": 150000,
    "totalStock": 100,
    "availableStock": 85,
    "saleStartAt": "2025-01-01T00:00:00",
    "saleEndAt": "2025-12-31T23:59:59",
    "event": {
      "id": 1,
      "name": "2025 아이유 콘서트",
      "eventDate": "2025-12-31T19:00:00"
    }
  }
]
```

#### 판매 중인 상품만 조회
```http
GET http://localhost:8080/api/products/available
Authorization: Bearer {accessToken}
```

#### 재고가 있는 상품만 조회
```http
GET http://localhost:8080/api/products/in-stock
Authorization: Bearer {accessToken}
```

#### 상품 상세 조회
```http
GET http://localhost:8080/api/products/{productId}
Authorization: Bearer {accessToken}
```

---

### 3. 대기열 (Queue Service) - Port 8083

#### 대기열 진입
```http
POST http://localhost:8083/queue/{eventId}/enqueue
Authorization: Bearer {accessToken}
```

**응답:**
```json
{
  "userKey": "event:1:user:123:20251020123456",
  "position": 42,
  "displayOffset": 43
}
```

- `position`: 실제 대기 순번 (0부터 시작)
- `displayOffset`: 사용자에게 보여줄 번호 (1부터 시작)

#### 대기열 상태 조회
```http
GET http://localhost:8083/queue/{eventId}/status
Authorization: Bearer {accessToken}
```

**응답:**
```json
{
  "position": 10,
  "passReady": false,
  "passToken": null
}
```

**통과 후:**
```json
{
  "position": 0,
  "passReady": true,
  "passToken": "pass:event:1:user:123:20251020123456"
}
```

---

### 4. 예매 (Ticket Service) - Port 8080

#### 예매 생성
```http
POST http://localhost:8080/api/reservations
Authorization: Bearer {accessToken}
X-Pass-Token: {passToken}
Content-Type: application/json

{
  "productId": 1,
  "quantity": 2
}
```

**중요:** `X-Pass-Token` 헤더 필수 (대기열 통과 후 받은 토큰)

**응답:**
```json
{
  "id": 1,
  "sagaId": "SAGA-20251020-123456",
  "userId": 123,
  "productId": 1,
  "productName": "VIP석",
  "quantity": 2,
  "totalPrice": 300000,
  "status": "PENDING",
  "createdAt": "2025-10-20T12:34:56"
}
```

#### 내 예매 내역 조회
```http
GET http://localhost:8080/api/reservations/my
Authorization: Bearer {accessToken}
```

**응답:**
```json
[
  {
    "id": 1,
    "sagaId": "SAGA-20251020-123456",
    "userId": 123,
    "productId": 1,
    "productName": "VIP석",
    "quantity": 2,
    "totalPrice": 300000,
    "status": "CONFIRMED",
    "createdAt": "2025-10-20T12:34:56"
  }
]
```

#### 예매 상세 조회
```http
GET http://localhost:8080/api/reservations/{reservationId}
Authorization: Bearer {accessToken}
```

#### 예매 취소
```http
PUT http://localhost:8080/api/reservations/{reservationId}/cancel
Authorization: Bearer {accessToken}
```

---

## 예매 플로우

### 전체 플로우

```
1. 로그인 (account-service)
   ↓
2. 상품 목록 조회 (ticket-service)
   ↓
3. 대기열 진입 (queue-service)
   ↓
4. 대기열 상태 폴링 (passReady = true 될 때까지)
   ↓
5. passToken 획득
   ↓
6. 예매 생성 (ticket-service + X-Pass-Token 헤더)
   ↓
7. 결제 자동 처리 (SAGA 패턴)
   ↓
8. 예매 완료 확인
```

### React 예제 코드

```javascript
// 1. 로그인
const login = async (email, password) => {
  const response = await fetch('http://localhost:8081/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  });
  const data = await response.json();
  localStorage.setItem('accessToken', data.accessToken);
  return data;
};

// 2. 상품 목록 조회
const getProducts = async () => {
  const response = await fetch('http://localhost:8080/api/products/available', {
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('accessToken')}`,
    },
  });
  return response.json();
};

// 3. 대기열 진입
const joinQueue = async (eventId) => {
  const response = await fetch(`http://localhost:8083/queue/${eventId}/enqueue`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('accessToken')}`,
    },
  });
  return response.json();
};

// 4. 대기열 상태 폴링
const pollQueueStatus = async (eventId) => {
  const interval = setInterval(async () => {
    const response = await fetch(`http://localhost:8083/queue/${eventId}/status`, {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('accessToken')}`,
      },
    });
    const data = await response.json();

    if (data.passReady) {
      clearInterval(interval);
      localStorage.setItem('passToken', data.passToken);
      // 예매 페이지로 이동
    } else {
      // 대기 번호 업데이트
      console.log(`현재 대기 순번: ${data.position}`);
    }
  }, 3000); // 3초마다 폴링
};

// 5. 예매 생성
const createReservation = async (productId, quantity) => {
  const response = await fetch('http://localhost:8080/api/reservations', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('accessToken')}`,
      'X-Pass-Token': localStorage.getItem('passToken'),
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ productId, quantity }),
  });
  return response.json();
};

// 6. 내 예매 내역 조회
const getMyReservations = async () => {
  const response = await fetch('http://localhost:8080/api/reservations/my', {
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('accessToken')}`,
    },
  });
  return response.json();
};
```

---

## 에러 처리

### HTTP 상태 코드

| 코드 | 의미 | 처리 방법 |
|------|------|-----------|
| 200 | 성공 | 정상 처리 |
| 400 | 잘못된 요청 | 입력값 검증 |
| 401 | 인증 실패 | 토큰 갱신 또는 재로그인 |
| 403 | 권한 없음 | Pass Token 필요 |
| 404 | 리소스 없음 | 존재하지 않는 상품/예매 |
| 409 | 충돌 (재고 부족 등) | 사용자에게 알림 |
| 500 | 서버 오류 | 관리자 문의 |

### 에러 응답 형식

```json
{
  "error": "INVALID_CREDENTIALS",
  "message": "이메일 또는 비밀번호가 올바르지 않습니다.",
  "timestamp": "2025-10-20T12:34:56"
}
```

### 주요 에러 케이스

#### 1. 토큰 만료
```javascript
if (response.status === 401) {
  // Refresh Token으로 갱신 시도
  const refreshResponse = await fetch('http://localhost:8081/api/auth/refresh', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      refreshToken: localStorage.getItem('refreshToken')
    }),
  });

  if (refreshResponse.ok) {
    const newTokens = await refreshResponse.json();
    localStorage.setItem('accessToken', newTokens.accessToken);
    // 원래 요청 재시도
  } else {
    // 로그인 페이지로 이동
    window.location.href = '/login';
  }
}
```

#### 2. Pass Token 없음 (403)
```javascript
if (response.status === 403) {
  alert('대기열을 통과해야 예매할 수 있습니다.');
  // 대기열 페이지로 이동
  window.location.href = `/queue/${eventId}`;
}
```

#### 3. 재고 부족 (409)
```javascript
if (response.status === 409) {
  const error = await response.json();
  alert(error.message); // "재고가 부족합니다."
}
```

---

## 개발 팁

### 1. 환경 변수 설정 (.env)

```env
REACT_APP_AUTH_API=http://localhost:8081
REACT_APP_TICKET_API=http://localhost:8080
REACT_APP_QUEUE_API=http://localhost:8083
```

### 2. Axios 인터셉터 설정

```javascript
import axios from 'axios';

const api = axios.create({
  baseURL: process.env.REACT_APP_TICKET_API,
});

// 요청 인터셉터 - 자동으로 토큰 추가
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 응답 인터셉터 - 401 시 자동 갱신
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      // 토큰 갱신 로직
      const refreshToken = localStorage.getItem('refreshToken');
      const response = await axios.post(
        'http://localhost:8081/api/auth/refresh',
        { refreshToken }
      );
      localStorage.setItem('accessToken', response.data.accessToken);
      // 재시도
      return api(error.config);
    }
    return Promise.reject(error);
  }
);

export default api;
```

### 3. 대기열 UI 권장사항

- 폴링 간격: **3초** (서버 부하 고려)
- 진행률 표시: `(총 대기열 크기 - 현재 순번) / 총 대기열 크기 * 100`
- Pass Token 유효시간: **5분** (표시 권장)

### 4. Postman 테스트

**Environment 설정:**
```
base_url: http://localhost:8081
access_token: (자동 설정)
pass_token: (수동 설정)
```

**Login API Test Script:**
```javascript
if (pm.response.code === 200) {
    const response = pm.response.json();
    pm.environment.set("access_token", response.accessToken);
}
```

---

## 문의 및 지원

- **백엔드 코드 위치**: `/Users/csh/project/flowgate`
- **API 변경 사항**: `docs/CHANGELOG.md` 참고
- **SAGA 패턴 상세**: `SAGA_PATTERN.md` 참고

개발 중 문제가 발생하면 백엔드 팀에 문의하거나 API 응답을 확인해주세요.
