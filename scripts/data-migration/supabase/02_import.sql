-- Atomic, one-time import. Run with psql -v ON_ERROR_STOP=1.
BEGIN;

DO $$
DECLARE
    expected RECORD;
    actual_count BIGINT;
BEGIN
    IF EXISTS (SELECT 1 FROM migration_source.import_batches WHERE batch_name = 'supabase-phase-5-v1') THEN
        RAISE EXCEPTION 'Phase 5 Supabase batch was already imported; refusing to run again.';
    END IF;

    FOR expected IN
        SELECT * FROM (VALUES
            ('profiles', 10), ('auth_users', 10), ('google_identities', 8),
            ('income', 19), ('monthly_expenses', 14), ('loans', 1),
            ('loan_payments', 13), ('international_transfers', 4),
            ('scheduled_payments', 1), ('transactions', 0)
        ) AS counts(table_name, expected_rows)
    LOOP
        EXECUTE format('SELECT count(*) FROM migration_source.%I', expected.table_name)
          INTO actual_count;
        IF actual_count <> expected.expected_rows THEN
            RAISE EXCEPTION 'Staging count mismatch for %: expected %, found %',
                expected.table_name, expected.expected_rows, actual_count;
        END IF;
    END LOOP;

    IF EXISTS (
        SELECT 1 FROM migration_source.profiles p
        FULL JOIN migration_source.auth_users a ON a.id = p.id
        WHERE p.id IS NULL OR a.id IS NULL
    ) THEN
        RAISE EXCEPTION 'Profile/Auth user UUID sets do not match exactly.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM migration_source.profiles p
        JOIN migration_source.auth_users a ON a.id = p.id
        WHERE p.email IS NOT NULL
          AND lower(trim(p.email)) <> lower(trim(a.email))
    ) THEN
        RAISE EXCEPTION 'A profile email differs from its Auth email; investigate before import.';
    END IF;

    IF EXISTS (
        SELECT 1 FROM migration_source.google_identities g
        LEFT JOIN migration_source.auth_users a ON a.id = g.user_id
        WHERE a.id IS NULL
    ) THEN
        RAISE EXCEPTION 'Google identity references an Auth user outside the 10 in-scope profiles.';
    END IF;

    IF EXISTS (
        SELECT 1 FROM migration_source.income s
        LEFT JOIN migration_source.auth_users a ON a.id = s.user_id
        WHERE a.id IS NULL
    ) OR EXISTS (
        SELECT 1 FROM migration_source.monthly_expenses s
        LEFT JOIN migration_source.auth_users a ON a.id = s.user_id
        WHERE a.id IS NULL
    ) OR EXISTS (
        SELECT 1 FROM migration_source.loans s
        LEFT JOIN migration_source.auth_users a ON a.id = s.user_id
        WHERE a.id IS NULL
    ) OR EXISTS (
        SELECT 1 FROM migration_source.loan_payments s
        LEFT JOIN migration_source.auth_users a ON a.id = s.user_id
        WHERE a.id IS NULL
    ) OR EXISTS (
        SELECT 1 FROM migration_source.international_transfers s
        LEFT JOIN migration_source.auth_users a ON a.id = s.user_id
        WHERE a.id IS NULL
    ) OR EXISTS (
        SELECT 1 FROM migration_source.scheduled_payments s
        LEFT JOIN migration_source.auth_users a ON a.id = s.user_id
        WHERE a.id IS NULL
    ) OR EXISTS (
        SELECT 1 FROM migration_source.transactions s
        LEFT JOIN migration_source.auth_users a ON a.id = s.user_id
        WHERE a.id IS NULL
    ) THEN
        RAISE EXCEPTION 'A financial row has an owner outside the in-scope Auth users.';
    END IF;

    IF EXISTS (
        SELECT 1 FROM migration_source.loan_payments p
        LEFT JOIN migration_source.loans l
          ON l.id = p.loan_id AND l.user_id = p.user_id
        WHERE l.id IS NULL
    ) THEN
        RAISE EXCEPTION 'Loan payment has no matching loan with the same owner.';
    END IF;

    IF EXISTS (
        SELECT 1 FROM public.app_users t
        JOIN migration_source.auth_users s ON s.id = t.id
    ) OR EXISTS (
        SELECT 1 FROM public.app_users t
        JOIN migration_source.auth_users s
          ON t.email_normalized = lower(trim(s.email))
    ) THEN
        RAISE EXCEPTION 'Target app_users already contains a source UUID or normalized email.';
    END IF;

    IF EXISTS (
        SELECT 1 FROM public.income t JOIN migration_source.income s ON s.id = t.id
    ) OR EXISTS (
        SELECT 1 FROM public.monthly_expenses t JOIN migration_source.monthly_expenses s ON s.id = t.id
    ) OR EXISTS (
        SELECT 1 FROM public.loans t JOIN migration_source.loans s ON s.id = t.id
    ) OR EXISTS (
        SELECT 1 FROM public.loan_payments t JOIN migration_source.loan_payments s ON s.id = t.id
    ) OR EXISTS (
        SELECT 1 FROM public.international_transfers t JOIN migration_source.international_transfers s ON s.id = t.id
    ) OR EXISTS (
        SELECT 1 FROM public.scheduled_payments t JOIN migration_source.scheduled_payments s ON s.id = t.id
    ) OR EXISTS (
        SELECT 1 FROM public.transactions t JOIN migration_source.transactions s ON s.id = t.id
    ) THEN
        RAISE EXCEPTION 'Target already contains one or more source financial UUIDs.';
    END IF;
END $$;

-- Preserve original Auth UUIDs, emails, verification state, and timestamps.
-- Password hashes are intentionally NULL: credentials are not exported.
INSERT INTO public.app_users
    (id, email, email_normalized, password_hash, email_verified_at, status, created_at, updated_at)
SELECT
    a.id, a.email, lower(trim(a.email)), NULL, a.email_confirmed_at, 'ACTIVE',
    a.created_at, a.updated_at
FROM migration_source.auth_users a;

INSERT INTO public.user_profiles
    (user_id, full_name, avatar_url, currency_preference, timezone, created_at, updated_at)
SELECT
    p.id, p.full_name, p.avatar_url,
    COALESCE(p.currency_preference, 'USD'),
    COALESCE(p.timezone, 'UTC'),
    p.created_at, p.updated_at
FROM migration_source.profiles p;

-- Target schema supports Google OAuth identities only. Email identities are
-- represented by the account email; passwords/sessions are not migrated.
INSERT INTO public.oauth_identities
    (id, user_id, provider, provider_subject, provider_email, created_at)
SELECT
    gen_random_uuid(), g.user_id, 'GOOGLE', g.provider_subject,
    g.provider_email, g.created_at
FROM migration_source.google_identities g;

INSERT INTO public.legacy_user_mappings
    (provider, legacy_user_id, app_user_id, created_at)
SELECT 'SUPABASE', a.id, a.id, a.created_at
FROM migration_source.auth_users a;

-- Insert financial records in FK dependency order, preserving every supplied
-- source column including historical dates, exact numerics and timestamps.
INSERT INTO public.income
    (id,user_id,source,amount,currency,category,description,date,is_recurring,
     recurring_frequency,created_at,updated_at,exchange_rate,inr_equivalent)
SELECT id,user_id,source,amount,currency,category,description,date,is_recurring,
       recurring_frequency,created_at,updated_at,exchange_rate,inr_equivalent
FROM migration_source.income;

INSERT INTO public.monthly_expenses
    (id,user_id,expense_name,amount,currency,exchange_rate,inr_equivalent,category,
     expense_date,payment_method,notes,created_at,updated_at)
SELECT id,user_id,expense_name,amount,currency,exchange_rate,inr_equivalent,category,
       expense_date,payment_method,notes,created_at,updated_at
FROM migration_source.monthly_expenses;

INSERT INTO public.loans
    (id,user_id,loan_name,principal_amount,current_balance,interest_rate,monthly_payment,
     start_date,end_date,lender,loan_type,status,created_at,updated_at,currency)
SELECT id,user_id,loan_name,principal_amount,current_balance,interest_rate,monthly_payment,
       start_date,end_date,lender,loan_type,status,created_at,updated_at,currency
FROM migration_source.loans;

INSERT INTO public.loan_payments
    (id,loan_id,user_id,amount,principal_amount,interest_amount,payment_date,payment_method,
     notes,created_at,currency,exchange_rate,inr_equivalent)
SELECT id,loan_id,user_id,amount,principal_amount,interest_amount,payment_date,payment_method,
       notes,created_at,currency,exchange_rate,inr_equivalent
FROM migration_source.loan_payments;

INSERT INTO public.international_transfers
    (id,user_id,recipient_name,recipient_country,amount_sent,currency_sent,amount_received,
     currency_received,exchange_rate,transfer_fee,transfer_method,purpose,status,transfer_date,
     completion_date,reference_number,created_at,updated_at,sent_currency,received_currency,
     usd_to_inr_rate,inr_equivalent)
SELECT id,user_id,recipient_name,recipient_country,amount_sent,currency_sent,amount_received,
       currency_received,exchange_rate,transfer_fee,transfer_method,purpose,status,transfer_date,
       completion_date,reference_number,created_at,updated_at,sent_currency,received_currency,
       usd_to_inr_rate,inr_equivalent
FROM migration_source.international_transfers;

INSERT INTO public.scheduled_payments
    (id,user_id,payment_name,amount,currency,category,recipient,due_date,frequency,is_recurring,
     auto_pay,status,reminder_days,notes,created_at,updated_at)
SELECT id,user_id,payment_name,amount,currency,category,recipient,due_date,frequency,is_recurring,
       auto_pay,status,reminder_days,notes,created_at,updated_at
FROM migration_source.scheduled_payments;

-- Transactions source is explicitly empty. This statement is intentionally
-- retained so any future non-empty source rows are imported, never synthesized.
INSERT INTO public.transactions
    (id,user_id,type,category,subcategory,amount,currency,description,merchant,
     transaction_date,payment_method,reference_id,tags,created_at,updated_at)
SELECT id,user_id,type,category,subcategory,amount,currency,description,merchant,
       transaction_date,payment_method,reference_id,tags,created_at,updated_at
FROM migration_source.transactions;

INSERT INTO migration_source.import_batches(batch_name, source_row_count)
VALUES ('supabase-phase-5-v1', 62);

COMMIT;
