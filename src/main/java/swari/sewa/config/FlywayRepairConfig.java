package swari.sewa.config;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Production Flyway migration strategy.
 * <p>
 * Uses the standard {@code migrate()} behavior only — no automatic
 * {@code repair()}. A failed migration stays in {@code flyway_schema_history}
 * with {@code success=0}, causing the application to fail to start. This
 * makes migration failures visible and prevents silent retries of
 * partially-applied migrations.
 * <p>
 * If a migration fails and the migration file has been fixed, run
 * {@code flyway repair} manually (or delete the failed row from
 * {@code flyway_schema_history}) before redeploying.
 */
@Configuration
@Profile("prod")
public class FlywayRepairConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return (flyway) -> flyway.migrate();
    }
}
