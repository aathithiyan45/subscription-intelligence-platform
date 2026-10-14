package com.aathithiyan.subscription.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FlywayMigrationIntegrationTest {

    @Autowired(required = false)
    private Flyway flyway;

    @Autowired
    private Environment environment;

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("Verify Hibernate ddl-auto is configured to validate")
    void testHibernateDdlAutoIsValidate() {
        String ddlAuto = environment.getProperty("spring.jpa.hibernate.ddl-auto");
        assertThat(ddlAuto)
                .as("Hibernate ddl-auto must be validate for production schema safety")
                .isEqualTo("validate");
    }

    @Test
    @DisplayName("Verify Flyway is enabled and has successfully baselined/migrated to V1")
    void testFlywayBaselineAndMigration() {
        assertThat(flyway)
                .as("Flyway bean must be configured and available in production context")
                .isNotNull();

        MigrationInfo current = flyway.info().current();
        assertThat(current)
                .as("Flyway should have an active migration record")
                .isNotNull();
        assertThat(current.getVersion().getVersion())
                .as("Current migration version must be at least 1")
                .isEqualTo("1");
        assertThat(current.getState().isApplied())
                .as("Migration state must be applied / successful")
                .isTrue();
    }

    @Test
    @DisplayName("Verify all expected tables and schema history exist in PostgreSQL")
    void testAllExpectedTablesExist() throws Exception {
        Set<String> tables = new HashSet<>();
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet rs = metaData.getTables(null, "public", "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME").toLowerCase());
                }
            }
        }

        assertThat(tables).contains(
                "flyway_schema_history",
                "users",
                "subscriptions",
                "price_histories",
                "notification_logs"
        );
    }
}
