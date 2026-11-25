-- V6: remove the redundant generic-transaction reporting index introduced by V5.
--
-- V4 already created idx_transactions_user_date on (user_id, transaction_date DESC).
-- The reporting query filters by owner and a date range, then checks the transaction
-- type. V5's (user_id, transaction_date, type) index repeats the V4 access prefix;
-- because transaction_date is a range predicate, type is not an efficient equality
-- prefix in that index. Keeping both adds write and storage cost without measured
-- evidence of a benefit.
--
-- Keep the V4 index for the current owner/date access pattern. If production-like
-- EXPLAIN (ANALYZE, BUFFERS) later shows that type filtering is a bottleneck, add a
-- measured replacement index with equality columns before the date range.

DROP INDEX IF EXISTS idx_transactions_user_date_type;
