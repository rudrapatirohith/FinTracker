-- Run after 02_import.sql. Any difference query below must return zero rows/counts.
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

\echo 'Imported source-owned target row counts'
SELECT 'income' AS table_name, count(*) FROM public.income t JOIN migration_source.income s USING (id)
UNION ALL SELECT 'monthly_expenses', count(*) FROM public.monthly_expenses t JOIN migration_source.monthly_expenses s USING (id)
UNION ALL SELECT 'loans', count(*) FROM public.loans t JOIN migration_source.loans s USING (id)
UNION ALL SELECT 'loan_payments', count(*) FROM public.loan_payments t JOIN migration_source.loan_payments s USING (id)
UNION ALL SELECT 'international_transfers', count(*) FROM public.international_transfers t JOIN migration_source.international_transfers s USING (id)
UNION ALL SELECT 'scheduled_payments', count(*) FROM public.scheduled_payments t JOIN migration_source.scheduled_payments s USING (id)
UNION ALL SELECT 'transactions', count(*) FROM public.transactions t JOIN migration_source.transactions s USING (id)
ORDER BY 1;

\echo 'Per-user counts: source versus imported target (zero rows means equal)'
WITH source_rows AS (
    SELECT user_id, 'income'::text AS table_name, count(*) AS row_count FROM migration_source.income GROUP BY user_id
    UNION ALL SELECT user_id, 'monthly_expenses', count(*) FROM migration_source.monthly_expenses GROUP BY user_id
    UNION ALL SELECT user_id, 'loans', count(*) FROM migration_source.loans GROUP BY user_id
    UNION ALL SELECT user_id, 'loan_payments', count(*) FROM migration_source.loan_payments GROUP BY user_id
    UNION ALL SELECT user_id, 'international_transfers', count(*) FROM migration_source.international_transfers GROUP BY user_id
    UNION ALL SELECT user_id, 'scheduled_payments', count(*) FROM migration_source.scheduled_payments GROUP BY user_id
    UNION ALL SELECT user_id, 'transactions', count(*) FROM migration_source.transactions GROUP BY user_id
), target_rows AS (
    SELECT t.user_id, 'income'::text AS table_name, count(*) AS row_count FROM public.income t JOIN migration_source.income s USING(id) GROUP BY t.user_id
    UNION ALL SELECT t.user_id, 'monthly_expenses', count(*) FROM public.monthly_expenses t JOIN migration_source.monthly_expenses s USING(id) GROUP BY t.user_id
    UNION ALL SELECT t.user_id, 'loans', count(*) FROM public.loans t JOIN migration_source.loans s USING(id) GROUP BY t.user_id
    UNION ALL SELECT t.user_id, 'loan_payments', count(*) FROM public.loan_payments t JOIN migration_source.loan_payments s USING(id) GROUP BY t.user_id
    UNION ALL SELECT t.user_id, 'international_transfers', count(*) FROM public.international_transfers t JOIN migration_source.international_transfers s USING(id) GROUP BY t.user_id
    UNION ALL SELECT t.user_id, 'scheduled_payments', count(*) FROM public.scheduled_payments t JOIN migration_source.scheduled_payments s USING(id) GROUP BY t.user_id
    UNION ALL SELECT t.user_id, 'transactions', count(*) FROM public.transactions t JOIN migration_source.transactions s USING(id) GROUP BY t.user_id
)
SELECT COALESCE(s.user_id,t.user_id) AS user_id, COALESCE(s.table_name,t.table_name) AS table_name,
       COALESCE(s.row_count,0) AS source_rows, COALESCE(t.row_count,0) AS target_rows
FROM source_rows s FULL JOIN target_rows t USING (user_id,table_name)
WHERE COALESCE(s.row_count,0) <> COALESCE(t.row_count,0)
ORDER BY 1,2;

\echo 'Full-row equality: raises an exception if any imported financial field differs'
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM (
        (SELECT * FROM migration_source.income EXCEPT ALL
         SELECT t.* FROM public.income t JOIN migration_source.income s USING(id))
        UNION ALL
        (SELECT t.* FROM public.income t JOIN migration_source.income s USING(id)
         EXCEPT ALL SELECT * FROM migration_source.income)
    ) d) THEN RAISE EXCEPTION 'Income row mismatch'; END IF;

    IF EXISTS (SELECT 1 FROM (
        (SELECT * FROM migration_source.monthly_expenses EXCEPT ALL
         SELECT t.* FROM public.monthly_expenses t JOIN migration_source.monthly_expenses s USING(id))
        UNION ALL
        (SELECT t.* FROM public.monthly_expenses t JOIN migration_source.monthly_expenses s USING(id)
         EXCEPT ALL SELECT * FROM migration_source.monthly_expenses)
    ) d) THEN RAISE EXCEPTION 'Monthly expense row mismatch'; END IF;

    IF EXISTS (SELECT 1 FROM (
        (SELECT * FROM migration_source.loans EXCEPT ALL
         SELECT t.* FROM public.loans t JOIN migration_source.loans s USING(id))
        UNION ALL
        (SELECT t.* FROM public.loans t JOIN migration_source.loans s USING(id)
         EXCEPT ALL SELECT * FROM migration_source.loans)
    ) d) THEN RAISE EXCEPTION 'Loan row mismatch'; END IF;

    IF EXISTS (SELECT 1 FROM (
        (SELECT * FROM migration_source.loan_payments EXCEPT ALL
         SELECT t.* FROM public.loan_payments t JOIN migration_source.loan_payments s USING(id))
        UNION ALL
        (SELECT t.* FROM public.loan_payments t JOIN migration_source.loan_payments s USING(id)
         EXCEPT ALL SELECT * FROM migration_source.loan_payments)
    ) d) THEN RAISE EXCEPTION 'Loan payment row mismatch'; END IF;

    IF EXISTS (SELECT 1 FROM (
        (SELECT * FROM migration_source.international_transfers EXCEPT ALL
         SELECT t.* FROM public.international_transfers t JOIN migration_source.international_transfers s USING(id))
        UNION ALL
        (SELECT t.* FROM public.international_transfers t JOIN migration_source.international_transfers s USING(id)
         EXCEPT ALL SELECT * FROM migration_source.international_transfers)
    ) d) THEN RAISE EXCEPTION 'International transfer row mismatch'; END IF;

    IF EXISTS (SELECT 1 FROM (
        (SELECT * FROM migration_source.scheduled_payments EXCEPT ALL
         SELECT t.* FROM public.scheduled_payments t JOIN migration_source.scheduled_payments s USING(id))
        UNION ALL
        (SELECT t.* FROM public.scheduled_payments t JOIN migration_source.scheduled_payments s USING(id)
         EXCEPT ALL SELECT * FROM migration_source.scheduled_payments)
    ) d) THEN RAISE EXCEPTION 'Scheduled payment row mismatch'; END IF;

    IF EXISTS (SELECT 1 FROM (
        (SELECT * FROM migration_source.transactions EXCEPT ALL
         SELECT t.* FROM public.transactions t JOIN migration_source.transactions s USING(id))
        UNION ALL
        (SELECT t.* FROM public.transactions t JOIN migration_source.transactions s USING(id)
         EXCEPT ALL SELECT * FROM migration_source.transactions)
    ) d) THEN RAISE EXCEPTION 'Transaction row mismatch'; END IF;
END $$;

\echo 'Exact financial totals by table and currency (source)'
SELECT 'income' AS table_name, currency, count(*) AS rows, sum(amount) AS amount_total,
       sum(inr_equivalent) AS inr_equivalent_total
FROM migration_source.income GROUP BY currency
UNION ALL
SELECT 'monthly_expenses', currency, count(*), sum(amount), sum(inr_equivalent)
FROM migration_source.monthly_expenses GROUP BY currency
UNION ALL
SELECT 'loan_payments', currency, count(*), sum(amount), sum(inr_equivalent)
FROM migration_source.loan_payments GROUP BY currency
UNION ALL
SELECT 'international_transfers_sent', currency_sent, count(*), sum(amount_sent), sum(inr_equivalent)
FROM migration_source.international_transfers GROUP BY currency_sent
UNION ALL
SELECT 'international_transfers_received', currency_received, count(*), sum(amount_received), sum(inr_equivalent)
FROM migration_source.international_transfers GROUP BY currency_received
UNION ALL
SELECT 'loans_principal', currency, count(*), sum(principal_amount), sum(current_balance)
FROM migration_source.loans GROUP BY currency
UNION ALL
SELECT 'scheduled_payments', currency, count(*), sum(amount), NULL::numeric
FROM migration_source.scheduled_payments GROUP BY currency
ORDER BY 1,2;

\echo 'Ownership and loan relationship violations (all must be zero)'
SELECT count(*) AS unmapped_financial_owners
FROM (
    SELECT user_id FROM public.income t JOIN migration_source.income s USING(id)
    UNION SELECT user_id FROM public.monthly_expenses t JOIN migration_source.monthly_expenses s USING(id)
    UNION SELECT user_id FROM public.loans t JOIN migration_source.loans s USING(id)
    UNION SELECT user_id FROM public.loan_payments t JOIN migration_source.loan_payments s USING(id)
    UNION SELECT user_id FROM public.international_transfers t JOIN migration_source.international_transfers s USING(id)
    UNION SELECT user_id FROM public.scheduled_payments t JOIN migration_source.scheduled_payments s USING(id)
    UNION SELECT user_id FROM public.transactions t JOIN migration_source.transactions s USING(id)
) f
LEFT JOIN public.legacy_user_mappings m
  ON m.provider = 'SUPABASE' AND m.legacy_user_id = f.user_id AND m.app_user_id = f.user_id
WHERE m.id IS NULL;

SELECT count(*) AS invalid_loan_payment_relationships
FROM public.loan_payments p
JOIN migration_source.loan_payments s ON s.id = p.id
LEFT JOIN public.loans l ON l.id = p.loan_id AND l.user_id = p.user_id
WHERE l.id IS NULL;
