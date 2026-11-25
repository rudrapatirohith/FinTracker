-- Run after Flyway V1-V6 on the target database.
-- Staging schema is isolated from application tables. No source PII is stored in Git.
CREATE SCHEMA IF NOT EXISTS migration_source;

CREATE TABLE IF NOT EXISTS migration_source.profiles (
    id UUID PRIMARY KEY,
    email TEXT,
    full_name TEXT,
    avatar_url TEXT,
    currency_preference TEXT,
    timezone TEXT,
    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ
);

-- LIKE copies the target's column definitions/defaults, not foreign keys.
-- The staging tables intentionally have no application ownership constraints.
CREATE TABLE IF NOT EXISTS migration_source.income (LIKE public.income INCLUDING DEFAULTS);
CREATE TABLE IF NOT EXISTS migration_source.monthly_expenses (LIKE public.monthly_expenses INCLUDING DEFAULTS);
CREATE TABLE IF NOT EXISTS migration_source.loans (LIKE public.loans INCLUDING DEFAULTS);
CREATE TABLE IF NOT EXISTS migration_source.loan_payments (LIKE public.loan_payments INCLUDING DEFAULTS);
CREATE TABLE IF NOT EXISTS migration_source.international_transfers (LIKE public.international_transfers INCLUDING DEFAULTS);
CREATE TABLE IF NOT EXISTS migration_source.scheduled_payments (LIKE public.scheduled_payments INCLUDING DEFAULTS);
CREATE TABLE IF NOT EXISTS migration_source.transactions (LIKE public.transactions INCLUDING DEFAULTS);

CREATE TABLE IF NOT EXISTS migration_source.auth_users (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    email_confirmed_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS migration_source.google_identities (
    user_id UUID NOT NULL,
    provider_subject VARCHAR(255) NOT NULL,
    provider_email VARCHAR(320),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    PRIMARY KEY (user_id, provider_subject),
    UNIQUE (provider_subject)
);

-- Source IDs must be unique in staging so duplicate exports fail before import.
CREATE UNIQUE INDEX IF NOT EXISTS uq_migration_source_income_id ON migration_source.income(id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_migration_source_monthly_expenses_id ON migration_source.monthly_expenses(id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_migration_source_loans_id ON migration_source.loans(id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_migration_source_loan_payments_id ON migration_source.loan_payments(id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_migration_source_international_transfers_id ON migration_source.international_transfers(id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_migration_source_scheduled_payments_id ON migration_source.scheduled_payments(id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_migration_source_transactions_id ON migration_source.transactions(id);

CREATE TABLE IF NOT EXISTS migration_source.import_batches (
    batch_name TEXT PRIMARY KEY,
    imported_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    source_row_count INTEGER NOT NULL
);

-- Do not truncate staging tables automatically: their contents are the evidence
-- used by the import and reconciliation scripts.
