-- Run after 02_import.sql. Expected results are documented in README.md.
\echo 'Source staging row counts'
SELECT 'profiles' AS table_name, count(*) FROM migration_source.profiles
UNION ALL SELECT 'auth_users', count(*) FROM migration_source.auth_users
UNION ALL SELECT 'google_identities', count(*) FROM migration_source.google_identities
UNION ALL SELECT 'income', count(*) FROM migration_source.income
UNION ALL SELECT 'monthly_expenses', count(*) FROM migration_source.monthly_expenses
UNION ALL SELECT 'loans', count(*) FROM migration_source.loans
UNION ALL SELECT 'loan_payments', count(*) FROM migration_source.loan_payments
UNION ALL SELECT 'international_transfers', count(*) FROM migration_source.international_transfers
UNION ALL SELECT 'scheduled_payments', count(*) FROM migration_source.scheduled_payments
UNION ALL SELECT 'transactions', count(*) FROM migration_source.transactions
ORDER BY 1;

\echo 'Imported financial row counts'
SELECT 'income' AS table_name, count(*) FROM public.income s JOIN migration_source.income m USING (id)
UNION ALL SELECT 'monthly_expenses', count(*) FROM public.monthly_expenses s JOIN migration_source.monthly_expenses m USING (id)
UNION ALL SELECT 'loans', count(*) FROM public.loans s JOIN migration_source.loans m USING (id)
UNION ALL SELECT 'loan_payments', count(*) FROM public.loan_payments s JOIN migration_source.loan_payments m USING (id)
UNION ALL SELECT 'international_transfers', count(*) FROM public.international_transfers s JOIN migration_source.international_transfers m USING (id)
UNION ALL SELECT 'scheduled_payments', count(*) FROM public.scheduled_payments s JOIN migration_source.scheduled_payments m USING (id)
UNION ALL SELECT 'transactions', count(*) FROM public.transactions s JOIN migration_source.transactions m USING (id)
ORDER BY 1;

\echo 'Per-user source and imported financial row counts'
WITH source_rows AS (
    SELECT user_id, 'income'::text AS table_name, count(*) AS row_count FROM migration_source.income GROUP BY user_id
    UNION ALL SELECT user_id, 'monthly_expenses', count(*) FROM migration_source.monthly_expenses GROUP BY user_id
    UNION ALL SELECT user_id, 'loans', count(*) FROM migration_source.loans GROUP BY user_id
    UNION ALL SELECT user_id, 'loan_payments', count(*) FROM migration_source.loan_payments GROUP BY user_id
    UNION ALL SELECT user_id, 'international_transfers', count(*) FROM migration_source.international_transfers GROUP BY user_id
    UNION ALL SELECT user_id, 'scheduled_payments', count(*) FROM migration_source.scheduled_payments GROUP BY user_id
    UNION ALL SELECT user_id, 'transactions', count(*) FROM migration_source.transactions GROUP BY user_id
), target_rows AS (
    SELECT user_id, 'income'::text AS table_name, count(*) AS row_count FROM public.income GROUP BY user_id
    UNION ALL SELECT user_id, 'monthly_expenses', count(*) FROM public.monthly_expenses GROUP BY user_id
    UNION ALL SELECT user_id, 'loans', count(*) FROM public.loans GROUP BY user_id
    UNION ALL SELECT user_id, 'loan_payments', count(*) FROM public.loan_payments GROUP BY user_id
    UNION ALL SELECT user_id, 'international_transfers', count(*) FROM public.international_transfers GROUP BY user_id
    UNION ALL SELECT user_id, 'scheduled_payments', count(*) FROM public.scheduled_payments GROUP BY user_id
    UNION ALL SELECT user_id, 'transactions', count(*) FROM public.transactions GROUP BY user_id
)
SELECT COALESCE(s.user_id,t.user_id) AS user_id, COALESCE(s.table_name,t.table_name) AS table_name,
       COALESCE(s.row_count,0) AS source_rows, COALESCE(t.row_count,0) AS target_rows
FROM source_rows s
FULL JOIN target_rows t USING (user_id,table_name)
WHERE COALESCE(s.row_count,0) <> COALESCE(t.row_count,0)
   OR COALESCE(s.row_count,0) > 0
ORDER BY 1,2;

\echo 'Exact financial totals by table and currency (source vs target)'
SELECT 'income' AS table_name, currency, count(*) AS rows, sum(amount) AS amount_total,
       sum(inr_equivalent) AS inr_equivalent_total
FROM migration_source.income GROUP BY currency
UNION ALL
SELECT 'income-target', currency, count(*), sum(amount), sum(inr_equivalent)
FROM public.income WHERE id IN (SELECT id FROM migration_source.income) GROUP BY currency
UNION ALL
SELECT 'monthly_expenses', currency, count(*), sum(amount), sum(inr_equivalent)
FROM migration_source.monthly_expenses GROUP BY currency
UNION ALL
SELECT 'monthly_expenses-target', currency, count(*), sum(amount), sum(inr_equivalent)
FROM public.monthly_expenses WHERE id IN (SELECT id FROM migration_source.monthly_expenses) GROUP BY currency
UNION ALL
SELECT 'loan_payments', currency, count(*), sum(amount), sum(inr_equivalent)
FROM migration_source.loan_payments GROUP BY currency
UNION ALL
SELECT 'loan_payments-target', currency, count(*), sum(amount), sum(inr_equivalent)
FROM public.loan_payments WHERE id IN (SELECT id FROM migration_source.loan_payments) GROUP BY currency
UNION ALL
SELECT 'international_transfers-sent', currency_sent, count(*), sum(amount_sent), sum(inr_equivalent)
FROM migration_source.international_transfers GROUP BY currency_sent
UNION ALL
SELECT 'international_transfers-sent-target', currency_sent, count(*), sum(amount_sent), sum(inr_equivalent)
FROM public.international_transfers WHERE id IN (SELECT id FROM migration_source.international_transfers) GROUP BY currency_sent
ORDER BY 1,2;

\echo 'Ownership and loan relationship violations (all must be zero)'
SELECT 'unmapped_financial_owners' AS check_name, count(*) AS violations
FROM (
    SELECT user_id FROM public.income WHERE id IN (SELECT id FROM migration_source.income)
    UNION SELECT user_id FROM public.monthly_expenses WHERE id IN (SELECT id FROM migration_source.monthly_expenses)
    UNION SELECT user_id FROM public.loans WHERE id IN (SELECT id FROM migration_source.loans)
    UNION SELECT user_id FROM public.loan_payments WHERE id IN (SELECT id FROM migration_source.loan_payments)
    UNION SELECT user_id FROM public.international_transfers WHERE id IN (SELECT id FROM migration_source.international_transfers)
    UNION SELECT user_id FROM public.scheduled_payments WHERE id IN (SELECT id FROM migration_source.scheduled_payments)
    UNION SELECT user_id FROM public.transactions WHERE id IN (SELECT id FROM migration_source.transactions)
) f
LEFT JOIN public.legacy_user_mappings m
  ON m.provider = 'SUPABASE' AND m.legacy_user_id = f.user_id AND m.app_user_id = f.user_id
WHERE m.id IS NULL;

SELECT count(*) AS invalid_loan_payment_relationships
FROM public.loan_payments p
JOIN migration_source.loan_payments s ON s.id = p.id
LEFT JOIN public.loans l ON l.id = p.loan_id AND l.user_id = p.user_id
WHERE l.id IS NULL;
