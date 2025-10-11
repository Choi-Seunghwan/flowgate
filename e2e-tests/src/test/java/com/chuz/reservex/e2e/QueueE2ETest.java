package com.chuz.reservex.e2e;

import com.chuz.reservex.account.AccountServiceApplication;
import com.chuz.reservex.queue.QueueServiceApplication;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.awaitility.Awaitility.await;

/**
 * 대기열 시스템 E2E 테스트
 *
 * 테스트 시나리오:
 * 1. 사용자 회원가입
 * 2. 로그인하여 JWT 토큰 발급
 * 3. JWT 토큰으로 대기열 진입
 * 4. 대기열 상태 폴링
 * 5. Pass Token 발급 확인
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(
    classes = {AccountServiceApplication.class, QueueServiceApplication.class},
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT
)
public class QueueE2ETest extends BaseE2ETest {

    private static final String TEST_EMAIL = "alice@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String TEST_NAME = "Alice";
    private static final Long EVENT_ID = 1L;

    private static String jwtToken;
    private static String passToken;

    @Test
    @Order(1)
    @DisplayName("1. 사용자 회원가입")
    void 회원가입() {
        Map<String, String> signupRequest = new HashMap<>();
        signupRequest.put("email", TEST_EMAIL);
        signupRequest.put("password", TEST_PASSWORD);
        signupRequest.put("name", TEST_NAME);

        given()
            .contentType(ContentType.JSON)
            .body(signupRequest)
        .when()
            .post(accountServiceUrl() + "/api/auth/signup")
        .then()
            .statusCode(200)
            .body("accessToken", notNullValue())
            .body("refreshToken", notNullValue())
            .body("tokenType", equalTo("Bearer"));

        System.out.println("✅ 회원가입 성공: " + TEST_EMAIL);
    }

    @Test
    @Order(2)
    @DisplayName("2. 로그인하여 JWT 토큰 발급")
    void 로그인() {
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", TEST_EMAIL);
        loginRequest.put("password", TEST_PASSWORD);

        jwtToken = given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
        .when()
            .post(accountServiceUrl() + "/api/auth/login")
        .then()
            .statusCode(200)
            .body("accessToken", notNullValue())
            .extract()
            .path("accessToken");

        System.out.println("✅ 로그인 성공");
        System.out.println("JWT Token: " + jwtToken.substring(0, 30) + "...");
    }

    @Test
    @Order(3)
    @DisplayName("3. JWT 토큰으로 대기열 진입")
    void 대기열_진입() {
        given()
            .header("Authorization", "Bearer " + jwtToken)
        .when()
            .post(queueServiceUrl() + "/queue/" + EVENT_ID + "/enqueue")
        .then()
            .statusCode(200)
            .body("userKey", notNullValue())
            .body("position", greaterThanOrEqualTo(0))
            .body("estimatedWaitSeconds", greaterThanOrEqualTo(0));

        System.out.println("✅ 대기열 진입 성공");
    }

    @Test
    @Order(4)
    @DisplayName("4. 대기열 상태 확인 및 Pass Token 발급 대기")
    void 대기열_상태_확인_및_Pass토큰_발급() {
        // Awaitility를 사용한 폴링 (최대 30초 대기)
        await()
            .atMost(30, TimeUnit.SECONDS)
            .pollInterval(2, TimeUnit.SECONDS)
            .until(() -> {
                String response = given()
                    .header("Authorization", "Bearer " + jwtToken)
                .when()
                    .get(queueServiceUrl() + "/queue/" + EVENT_ID + "/status")
                .then()
                    .statusCode(200)
                    .extract()
                    .asString();

                // canProceed가 true인지 확인
                return response.contains("\"canProceed\":true");
            });

        // Pass Token 추출
        passToken = given()
            .header("Authorization", "Bearer " + jwtToken)
        .when()
            .get(queueServiceUrl() + "/queue/" + EVENT_ID + "/status")
        .then()
            .statusCode(200)
            .body("canProceed", equalTo(true))
            .body("passToken", notNullValue())
            .extract()
            .path("passToken");

        System.out.println("✅ Pass Token 발급 성공");
        System.out.println("Pass Token: " + passToken);
    }

    @Test
    @Order(5)
    @DisplayName("5. 중복 대기열 진입 시 기존 위치 반환")
    void 중복_대기열_진입() {
        given()
            .header("Authorization", "Bearer " + jwtToken)
        .when()
            .post(queueServiceUrl() + "/queue/" + EVENT_ID + "/enqueue")
        .then()
            .statusCode(200)
            .body("userKey", notNullValue());

        System.out.println("✅ 중복 진입 처리 확인");
    }

    @Test
    @Order(6)
    @DisplayName("6. JWT 토큰 없이 대기열 진입 시도 (실패)")
    void 인증없이_대기열_진입_실패() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .post(queueServiceUrl() + "/queue/" + EVENT_ID + "/enqueue")
        .then()
            .statusCode(anyOf(equalTo(401), equalTo(403)));

        System.out.println("✅ 인증 없는 접근 차단 확인");
    }
}
