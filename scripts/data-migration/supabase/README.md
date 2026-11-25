# Supabase → FinTracker PostgreSQL data migration

This is a one-time, operator-run migration. It is deliberately separate from Flyway: source exports and user-specific data must never be committed to the repository.

## Scope captured for this migration

Expected source counts (from the unfiltered Supabase exports supplied for this migration):

| Source table | Rows |
|---|---:|
| profiles | 10 |
| income | 19 |
| monthly_expenses | 14 |
| loans | 1 |
| loan_payments | 13 |
| international_transfers | 4 |
| scheduled_payments | 1 |
| transactions | 0 |
| **Total** | **62** |

The 10 profiles correspond to 10 Supabase Auth users. An additional Auth-only account was explicitly excluded after confirming it owns no financial rows. Do not delete or modify that account in Supabase.

Auth identity inventory: 10 users, 11 identities (8 Google identity rows and 3 email identity rows; one user has both email and Google). The target schema stores Google identities in `oauth_identities`; email/password login is represented by `app_users.email` and a nullable password hash. Supabase password hashes and sessions are not migrated. Users requiring password login must use the new application's password recovery/reset flow. Existing Google provider subject IDs are imported unchanged.

## Safety rules

- Keep the original Supabase SQL exports and Auth CSV exports outside Git. Never commit them, credentials, access tokens, or database URLs.
- Take a verified PostgreSQL backup of the target database before running anything.
- Run only against a dedicated target database/environment that has successfully applied Flyway V1–V6.
- Keep Supabase as the source of truth until all reconciliation and application smoke tests pass.
- Do not run this against a target containing live financial data. The import intentionally refuses pre-existing target rows for the source UUIDs.
- The scripts do not connect to Supabase and do not modify Supabase.
- Run the import once. The batch marker prevents accidental re-import.
- Rollback is destructive to imported rows; only use it before application writes have begun and after reviewing the rollback preview.

## Files

1. `01_create_staging.sql` — creates isolated source staging tables in schema `migration_source`.
2. `02_import.sql` — validates source identities/relationships and atomically imports the 52 financial rows plus 10 profiles (and their identity records).
3. `03_reconcile.sql` — reports source-vs-target counts, per-user counts, exact financial totals grouped by currency, and full-row equality checks.
4. `04_rollback.sql` — preview and explicit rollback of the migration batch.

## Prepare source exports

1. In Supabase, export each of the seven populated tables as SQL without filters. The `transactions` export is intentionally empty.
2. Keep all original exports and the two Auth CSV files in `scripts/data-migration/supabase/private/` (this directory is Git-ignored). Do not paste secrets or commit these files.
3. Keep the original export files unchanged as your source evidence.
4. Create one combined working SQL file from the seven table exports (profiles plus the six populated financial tables). In that working copy only, replace the prefix `INSERT INTO public.` with `INSERT INTO migration_source.`. Do not globally replace the word `public` or alter values/column lists.
5. Execute `01_create_staging.sql` on the target database.
6. Execute the transformed combined INSERT statements on the target database. The script creates staging tables with the target's compatible column types; source rows remain isolated in `migration_source`.
7. Export two CSV files from the read-only Auth queries: one row per in-scope Auth user, and one row per Google identity. Include headers. Use the exact column order specified in the `auth_users` and `google_identities` staging table definitions in `01_create_staging.sql`. Load them using psql `\copy` commands, or use a trusted PostgreSQL client import. The Auth users CSV must include only the 10 users that have a profile; do not include the excluded Auth-only account. The Google CSV must contain the eight Google rows. Email identity rows are not OAuth identities in the target.
8. Verify staging counts before proceeding: profiles 10, income 19, monthly_expenses 14, loans 1, loan_payments 13, international_transfers 4, scheduled_payments 1, transactions 0, auth_users 10, google_identities 8.

Use these read-only queries to create the two CSV files (with headers). They reproduce the required column order exactly:

```sql
-- auth_users.csv: exactly the 10 users linked to profiles
SELECT u.id, u.email, u.created_at, u.updated_at, u.email_confirmed_at
FROM auth.users u
JOIN public.profiles p ON p.id = u.id
ORDER BY u.id;

-- google_identities.csv: exactly the eight Google identities linked to profiles
SELECT i.user_id, i.provider_id AS provider_subject,
       i.identity_data ->> 'email' AS provider_email, i.created_at
FROM auth.identities i
JOIN public.profiles p ON p.id = i.user_id
WHERE i.provider = 'google'
ORDER BY i.user_id;
```

CSV column order:
- auth_users: `id,email,created_at,updated_at,email_confirmed_at`
- google_identities: `user_id,provider_subject,provider_email,created_at`

For example, with psql and files stored in the private ignored directory, load them in this order:
```sql
\copy migration_source.auth_users(id,email,created_at,updated_at,email_confirmed_at) FROM 'scripts/data-migration/supabase/private/auth-users.csv' WITH (FORMAT csv, HEADER true);
\copy migration_source.google_identities(user_id,provider_subject,provider_email,created_at) FROM 'scripts/data-migration/supabase/private/google-identities.csv' WITH (FORMAT csv, HEADER true);
```

Timestamps must be exported in ISO-8601 with timezone offsets. Keep original UUIDs and decimal values. Do not calculate or normalize financial values.

## Backup

Before creating staging or importing, take a custom-format backup and verify that it can be listed/restored in a non-production test database:

```powershell
pg_dump "$env:FINTRACKER_MIGRATION_DATABASE_URL" --format=custom --file .\\scripts\\data-migration\\supabase\\private\\fintracker-before-phase5.dump
pg_restore --list .\\scripts\\data-migration\\supabase\\private\\fintracker-before-phase5.dump
```

## Dry run and execute

First run the complete import in dry-run mode. The script executes all inserts and database constraints inside one transaction, then rolls the transaction back:

```powershell
psql "$env:FINTRACKER_MIGRATION_DATABASE_URL" -v ON_ERROR_STOP=1 -v dry_run=true -f .\scripts\data-migration\supabase\02_import.sql
```

If the dry run succeeds, review the output and run the same script with `dry_run=false` to commit:

```powershell
psql "$env:FINTRACKER_MIGRATION_DATABASE_URL" -v ON_ERROR_STOP=1 -v dry_run=false -f .\scripts\data-migration\supabase\02_import.sql
```

The batch marker prevents a second committed import. A dry run does not create the marker because its transaction is rolled back.

Run `02_import.sql` in a single psql session with `ON_ERROR_STOP=1`, e.g.:

```powershell
psql "$env:FINTRACKER_MIGRATION_DATABASE_URL" -v ON_ERROR_STOP=1 -f .\scripts\data-migration\supabase\02_import.sql
```

Then run `03_reconcile.sql` and inspect every result. The supplied dataset contains 52 financial rows plus 10 profile rows (62 source rows total). In addition, import creates 10 app users, 10 legacy mappings, and 8 Google OAuth identity rows. The source `transactions` count must remain zero.

The target `oauth_identities` table has no `updated_at` column, so the source Google identity `created_at` is preserved; identity `updated_at` is retained in the private source export as audit evidence but has no target column. The target schema also has no profile email column: Auth email is authoritative for `app_users.email`; profile email is not copied. The import rejects profile/Auth email mismatches after case/whitespace normalization rather than silently choosing one.

The source `auth.users.updated_at` value is preserved as `app_users.updated_at`; profile timestamps are preserved in `user_profiles`. Email verification timestamps, including NULL for an unverified user, are preserved exactly.

## Post-import acceptance

- All row counts match.
- No orphan user IDs; every financial owner has a legacy mapping.
- Every loan payment references the correct imported loan and same owner.
- Per-user counts match.
- Exact numeric totals reconcile per currency and table.
- No synthetic transaction records exist.
- Login, Google OAuth, profile, dashboard, and financial CRUD smoke tests pass.
- Keep Supabase read-only/source-of-truth until acceptance is signed off.
- After acceptance and after retaining the required backup/evidence, remove the staging copy with `05_cleanup_staging.sql`.

Do not delete the Supabase Auth-only account or source records as part of this migration.
