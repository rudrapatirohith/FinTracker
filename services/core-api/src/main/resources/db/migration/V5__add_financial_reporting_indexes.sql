-- Index the owner/date access paths used by dashboard and reporting queries.
-- These are additive and preserve the existing financial records and constraints.

CREATE INDEX IF NOT EXISTS idx_income_user_date
    ON income (user_id, date);

CREATE INDEX IF NOT EXISTS idx_monthly_expenses_user_date
    ON monthly_expenses (user_id, expense_date);

CREATE INDEX IF NOT EXISTS idx_loan_payments_user_date
    ON loan_payments (user_id, payment_date);

CREATE INDEX IF NOT EXISTS idx_transfers_user_date
    ON international_transfers (user_id, transfer_date);

CREATE INDEX IF NOT EXISTS idx_scheduled_payments_user_status_due_date
    ON scheduled_payments (user_id, status, due_date);

CREATE INDEX IF NOT EXISTS idx_transactions_user_date_type
    ON transactions (user_id, transaction_date, type);

CREATE INDEX IF NOT EXISTS idx_loans_user_start_date
    ON loans (user_id, start_date);
