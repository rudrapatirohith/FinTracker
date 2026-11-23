-- Purpose: help PostgreSQL find the rows used by the reporting queries faster.
--
-- A database index is like an index at the back of a book: it helps PostgreSQL
-- locate matching rows without checking every row in a large table. These are
-- composite indexes, meaning one index stores more than one column.
--
-- The reporting queries always start by selecting one user's records. They then
-- filter those records by status/date, type/date, or loan start date. Column order
-- matters: equality filters generally belong before a date-range column in a
-- composite index. These indexes should still be validated with EXPLAIN ANALYZE
-- against representative data before claiming a measured performance improvement.
--
-- V4 already creates owner/date indexes for income, monthly expenses, loan
-- payments, transfers, and general transactions. V5 adds the missing scheduled
-- payment and loan access paths, plus a transaction index that also includes type.
--
-- IF NOT EXISTS only avoids creating another index with the same name. It does
-- not detect an equivalent index that exists under a different name.

-- The report counts scheduled payments only when paid, and only within due_date.
CREATE INDEX IF NOT EXISTS idx_scheduled_payments_user_status_due_date
    ON scheduled_payments (user_id, status, due_date);

-- The report filters generic transactions by owner and type, then by a date range.
-- Equality columns come before the range column so PostgreSQL can use the leading
-- index columns efficiently. This overlaps the V4 owner/date index by design;
-- compare query plans and write overhead before keeping both long term.
CREATE INDEX IF NOT EXISTS idx_transactions_user_type_date
    ON transactions (user_id, type, transaction_date);

-- The loan snapshot selects the owner's loans that started on or before the report end date.
CREATE INDEX IF NOT EXISTS idx_loans_user_start_date
    ON loans (user_id, start_date);
