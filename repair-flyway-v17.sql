-- Repair Flyway schema history for failed V17 migration
DELETE FROM flyway_schema_history WHERE version = '17';
