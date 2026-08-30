package swari.sewa.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("prod")
public class FlywayRepairConfig {

    /**
     * Repairs the Flyway schema history before migrating.
     * This is useful when a previous migration failed (e.g. due to TiDB not
     * supporting stored procedures) and the migration file has been fixed.
     */
    @Bean
    public FlywayMigrationStrategy repairAndMigrateStrategy() {
        return (Flyway flyway) -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}
