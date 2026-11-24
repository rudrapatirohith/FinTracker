-- Add the remaining indexes needed by the dashboard/reporting read queries.
--
-- Why these columns and this order?
-- The reporting queries first isolate one user's rows (user_id), then narrow
-- the matching records by a status/type/date condition. A composite index can
-- help PostgreSQL locate those rows without scanning the entire table.
--
-- V4 already added the basic owner/date indexes for income, monthly expenses,
-- loan payments, and transfers. This migration adds the remaining indexes
-- used by the scheduled-payment, generic-transaction, and loan queries.
--
-- IF NOT EXISTS makes the migration safe if an equivalent index is already present.

-- The report counts scheduled payments only when paid, and only within due_date.
CREATE INDEX IF NOT EXISTS idx_scheduled_payments_user_status_due_date
    ON scheduled_payments (user_id, status, due_date);

-- The report filters generic transactions by owner, date range, and income/expense type.
CREATE INDEX IF NOT EXISTS idx_transactions_user_date_type
    ON transactions (user_id, transaction_date, type);

-- The loan snapshot selects the owner's loans that started on or before the report end date.
CREATE INDEX IF NOT EXISTS idx_loans_user_start_date
    ON loans (user_id, start_date);
