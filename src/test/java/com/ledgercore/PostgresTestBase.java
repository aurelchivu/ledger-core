package com.ledgercore;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Shared Postgres Testcontainers base for integration tests.
 *
 * Key traits:
 * - One container per test JVM (fast).
 * - DynamicPropertySource wires Spring Boot's DataSource + Flyway to the container.
 */
public abstract class PostgresTestBase {

    // NOTE: Use a static container to share across all tests in the same JVM.
    // If we later enable parallel test forks, reconsider this (see notes below).
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("ledger")
                    .withUsername("ledger")
                    .withPassword("ledger");

    static {
        // NOTE: Starts once, before any @SpringBootTest context loads.
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void configureDb(DynamicPropertyRegistry registry) {
        // --- DataSource (Spring uses these) ---
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        // --- Flyway ---
        // IMPORTANT: Ensure Flyway is enabled during tests even if someone disables it in main props later.
        registry.add("spring.flyway.enabled", () -> "true");

        // OPTIONAL: Be explicit about locations so tests don't depend on main app properties.
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
    }
}
