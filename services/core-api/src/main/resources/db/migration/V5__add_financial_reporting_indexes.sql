-- Add the remaining owner/date indexes used by dashboard and reporting queries.
-- V4 already creates the basic owner/date indexes for income, monthly expenses,
-- loan payments, and transfers. These indexes add status/type/start-date selectivity.

CREATE INDEX IF NOT EXISTS idx_scheduled_payments_user_status_due_date
    ON scheduled_payments (user_id, status, due_date);

CREATE INDEX IF NOT EXISTS idx_transactions_user_date_type
    ON transactions (user_id, transaction_date, type);

CREATE INDEX IF NOT EXISTS idx_loans_user_start_date
    ON loans (user_id, start_date);
