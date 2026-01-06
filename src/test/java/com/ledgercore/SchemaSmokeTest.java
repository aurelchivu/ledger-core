package com.ledgercore;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test:
 * - proves the Spring context can boot with a REAL Postgres
 * - proves Flyway migrations executed (not just "tables happen to exist")
 *
 */
@SpringBootTest
class SchemaSmokeTest extends PostgresTestBase {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void schema_is_applied_and_core_tables_exist() {
        // --- 1) Verify we are on Postgres (not H2, not some local accidental DB) ---
        String dbProduct = jdbc.queryForObject("select version()", String.class);
        assertThat(dbProduct)
                .as("Expected to connect to a real PostgreSQL instance (Testcontainers), but got: %s", dbProduct)
                .contains("PostgreSQL");

        // --- 2) Verify Flyway actually ran ---
        // Flyway creates flyway_schema_history and writes applied migrations.
        Integer flywayHistoryExists = jdbc.queryForObject("""
        select count(*)
        from information_schema.tables
        where table_schema = 'public'
          and table_name = 'flyway_schema_history'
        """, Integer.class);

        assertThat(flywayHistoryExists)
                .as("Flyway schema history table missing -> Flyway likely did not run")
                .isEqualTo(1);

        // Ensure at least version 1 is applied (the V1__... migration)
        Integer appliedV1 = jdbc.queryForObject("""
        select count(*)
        from flyway_schema_history
        where version = '1' and success = true
        """, Integer.class);

        assertThat(appliedV1)
                .as("Expected Flyway migration version '1' to be applied successfully, but it was not")
                .isEqualTo(1);

        // --- 3) Verify the core tables exist in the expected schema ---
        List<String> expectedTables = List.of("accounts", "ledger_entries", "outbox_events", "commands", "transfers");

        for (String table : expectedTables) {
            Integer exists = jdbc.queryForObject("""
          select count(*)
          from information_schema.tables
          where table_schema = 'public'
            and table_name = ?
          """, Integer.class, table);

            assertThat(exists)
                    .as("Expected table '%s' to exist in schema 'public'", table)
                    .isEqualTo(1);
        }
    }
}
