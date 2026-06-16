package com.example.booking;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for integration tests. Boots a real PostgreSQL instance via
 * Testcontainers so tests run against the same database engine as production,
 * including its real locking semantics (which matter for the concurrency tests
 * arriving in Segment 3). Requires a running Docker daemon.
 *
 * <p>Deliberately NOT using {@code @Testcontainers}/{@code @Container}: that
 * combination manages the container's start/stop per test <em>class</em>, but
 * since every integration test class shares this same static field and Spring
 * caches a single ApplicationContext (and therefore one JDBC connection pool)
 * across them, a stop-then-restart between classes hands out a new container
 * port that the already-cached DataSource doesn't know about -- every test
 * class after the first restart fails with "connection refused". Starting the
 * container exactly once in a static initializer (the documented Testcontainers
 * "singleton container" pattern) avoids that: it stays up for the whole JVM and
 * is reaped by Testcontainers' Ryuk container when the test run ends.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
