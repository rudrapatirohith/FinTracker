-- DESTRUCTIVE: review the preview first. Only use before application writes begin.
-- Run with psql -v ON_ERROR_STOP=1. Uncomment the transaction block only after review.

SELECT 'income' AS table_name, count(*) AS rows_to_delete FROM public.income WHERE id IN (SELECT id FROM migration_source.income)
UNION ALL SELECT 'monthly_expenses', count(*) FROM public.monthly_expenses WHERE id IN (SELECT id FROM migration_source.monthly_expenses)
UNION ALL SELECT 'loan_payments', count(*) FROM public.loan_payments WHERE id IN (SELECT id FROM migration_source.loan_payments)
UNION ALL SELECT 'loans', count(*) FROM public.loans WHERE id IN (SELECT id FROM migration_source.loans)
UNION ALL SELECT 'international_transfers', count(*) FROM public.international_transfers WHERE id IN (SELECT id FROM migration_source.international_transfers)
UNION ALL SELECT 'scheduled_payments', count(*) FROM public.scheduled_payments WHERE id IN (SELECT id FROM migration_source.scheduled_payments)
UNION ALL SELECT 'transactions', count(*) FROM public.transactions WHERE id IN (SELECT id FROM migration_source.transactions)
UNION ALL SELECT 'oauth_identities', count(*) FROM public.oauth_identities WHERE user_id IN (SELECT id FROM migration_source.auth_users)
UNION ALL SELECT 'legacy_user_mappings', count(*) FROM public.legacy_user_mappings WHERE provider='SUPABASE' AND legacy_user_id IN (SELECT id FROM migration_source.auth_users)
UNION ALL SELECT 'user_profiles', count(*) FROM public.user_profiles WHERE user_id IN (SELECT id FROM migration_source.auth_users)
UNION ALL SELECT 'app_users', count(*) FROM public.app_users WHERE id IN (SELECT id FROM migration_source.auth_users);

-- After confirming that the preview contains only this batch, execute:
-- BEGIN;
-- DELETE FROM public.loan_payments WHERE id IN (SELECT id FROM migration_source.loan_payments);
-- DELETE FROM public.loans WHERE id IN (SELECT id FROM migration_source.loans);
-- DELETE FROM public.income WHERE id IN (SELECT id FROM migration_source.income);
-- DELETE FROM public.monthly_expenses WHERE id IN (SELECT id FROM migration_source.monthly_expenses);
-- DELETE FROM public.international_transfers WHERE id IN (SELECT id FROM migration_source.international_transfers);
-- DELETE FROM public.scheduled_payments WHERE id IN (SELECT id FROM migration_source.scheduled_payments);
-- DELETE FROM public.transactions WHERE id IN (SELECT id FROM migration_source.transactions);
-- DELETE FROM public.oauth_identities WHERE user_id IN (SELECT id FROM migration_source.auth_users);
-- DELETE FROM public.legacy_user_mappings WHERE provider='SUPABASE' AND legacy_user_id IN (SELECT id FROM migration_source.auth_users);
-- DELETE FROM public.user_profiles WHERE user_id IN (SELECT id FROM migration_source.auth_users);
-- DELETE FROM public.app_users WHERE id IN (SELECT id FROM migration_source.auth_users);
-- DELETE FROM migration_source.import_batches WHERE batch_name='supabase-phase-5-v1';
-- COMMIT;
