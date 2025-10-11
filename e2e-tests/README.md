# E2E Tests

ReserveX 프로젝트의 End-to-End 통합 테스트 모듈입니다.

## 기술 스택

- **REST Assured**: REST API 테스트 프레임워크
- **TestContainers**: Docker 기반 통합 테스트 환경
- **Awaitility**: 비동기 폴링 테스트
- **JUnit 5**: 테스트 프레임워크

## 테스트 실행

### 전체 E2E 테스트 실행
```bash
./gradlew :e2e-tests:e2eTest
```

### 일반 테스트 포함 모두 실행
```bash
./gradlew :e2e-tests:test
```

## 테스트 구조

```
e2e-tests/
├── src/test/java/com/chuz/reservex/e2e/
│   ├── BaseE2ETest.java          # 베이스 클래스 (TestContainers 설정)
│   └── QueueE2ETest.java         # 대기열 E2E 테스트
└── src/test/resources/
    └── application-test.yml      # 테스트 설정
```

## 주요 테스트 시나리오

### QueueE2ETest
로그인부터 대기열 통과까지의 전체 플로우를 테스트합니다:

1. ✅ 사용자 회원가입
2. ✅ 로그인하여 JWT 토큰 발급
3. ✅ JWT 토큰으로 대기열 진입
4. ✅ 대기열 상태 확인 및 Pass Token 발급 대기 (폴링)
5. ✅ 중복 대기열 진입 시 기존 위치 반환
6. ✅ 인증 없이 대기열 진입 시도 (실패 케이스)

## TestContainers

테스트 실행 시 자동으로 다음 컨테이너들이 시작됩니다:
- **PostgreSQL 16**: account-service용 데이터베이스
- **Redis 7**: queue-service용 캐시

## REST Assured 예제

```java
// 로그인 요청
String token = given()
    .contentType(ContentType.JSON)
    .body(loginRequest)
.when()
    .post("/api/auth/login")
.then()
    .statusCode(200)
    .body("accessToken", notNullValue())
    .extract()
    .path("accessToken");

// JWT 인증 헤더와 함께 대기열 진입
given()
    .header("Authorization", "Bearer " + token)
.when()
    .post("/queue/1/enqueue")
.then()
    .statusCode(200)
    .body("position", greaterThanOrEqualTo(0));
```

## Awaitility 폴링 예제

대기열 상태를 폴링하여 Pass Token 발급을 기다립니다:

```java
await()
    .atMost(30, TimeUnit.SECONDS)
    .pollInterval(2, TimeUnit.SECONDS)
    .until(() -> {
        String response = given()
            .header("Authorization", "Bearer " + jwtToken)
            .get("/queue/1/status")
            .asString();

        return response.contains("\"canProceed\":true");
    });
```

## 실무 팁

### 1. 테스트 격리
각 테스트는 독립적으로 실행 가능해야 합니다. `@TestMethodOrder`를 사용하되, 테스트 간 의존성을 최소화하세요.

### 2. 테스트 속도
TestContainers는 시작 시간이 걸립니다. `static` 필드로 컨테이너를 공유하여 재사용하세요.

### 3. 환경 변수
민감한 정보는 환경 변수나 테스트 프로필로 관리하세요.

### 4. CI/CD 통합
GitHub Actions, Jenkins 등에서 Docker를 사용할 수 있다면 그대로 실행 가능합니다.

## 추가 테스트 작성

새로운 E2E 테스트를 작성하려면:

1. `BaseE2ETest`를 상속받습니다
2. `@Tag("e2e")`를 추가합니다
3. REST Assured를 사용하여 API 호출을 작성합니다

```java
@Tag("e2e")
@SpringBootTest(classes = {YourServiceApplication.class})
public class YourE2ETest extends BaseE2ETest {

    @Test
    void 테스트_시나리오() {
        given()
            .when()
            .then();
    }
}
```
