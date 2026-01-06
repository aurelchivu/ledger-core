package com.ledgercore;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

public abstract class PostgresTestBase {

    // NOTE: One container shared per test JVM to keep tests fast.
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("ledger")
                    .withUsername("ledger")
                    .withPassword("ledger");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void configureDb(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        // Ensures Flyway runs against the container DB during tests
        registry.add("spring.flyway.enabled", () -> "true");
    }
}
