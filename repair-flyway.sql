-- Repair Flyway migration by removing failed V7 and V8 migrations
DELETE FROM flyway_schema_history WHERE version = 7;
DELETE FROM flyway_schema_history WHERE version = 8;
