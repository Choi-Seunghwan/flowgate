package com.chuz.reservex.e2e;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * E2E 테스트 베이스 클래스
 * TestContainers를 사용하여 실제 환경과 유사한 테스트 환경 구축
 */
@Tag("e2e")
@Testcontainers
@ActiveProfiles("test")
public abstract class BaseE2ETest {

    // PostgreSQL Container (account-service용)
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("reservex_test")
            .withUsername("test")
            .withPassword("test");

    // Redis Container (queue-service용)
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    static {
        postgres.start();
        redis.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL 설정
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // Redis 설정
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));

        // JWT 설정
        registry.add("jwt.secret", () -> "test-secret-key-must-be-at-least-256-bits-long-for-HS256-algorithm-security");
    }

    @LocalServerPort
    protected int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
    }

    /**
     * 서비스별 베이스 URL
     */
    protected String accountServiceUrl() {
        return "http://localhost:8081";
    }

    protected String queueServiceUrl() {
        return "http://localhost:8083";
    }

    protected String ticketServiceUrl() {
        return "http://localhost:8080";
    }
}
