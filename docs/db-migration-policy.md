# DB Migration Policy

This project uses Flyway as the single source of truth for schema evolution.

## Rules

1. Never change existing migration files that have already run in shared environments.
2. Every schema change must be a new migration in `src/main/resources/db/migration`.
3. Migration names must be deterministic and descriptive:
   - `V3__add_xxx_column.sql`
   - `V4__create_yyy_table.sql`
4. Do not rely on JPA auto-DDL for schema changes (`ddl-auto` remains `validate`).
5. Application startup must succeed from an empty DB using only Flyway migrations.

## Verification Checklist

1. Run the app on a clean database and verify Flyway applies all migrations.
2. Confirm `flyway_schema_history` contains expected versions in order.
3. Run:
   - `mvn -q test`
   - `mvn -q -DskipTests package`
4. Smoke-test critical endpoints after migration.

## Data Seeding

Use `docs/dev-seed.sql` only for local development/test data.
It must stay idempotent (`INSERT ... ON CONFLICT ...`) and never be treated as production migration.
