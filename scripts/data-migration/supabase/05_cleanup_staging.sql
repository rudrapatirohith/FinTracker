-- Run only after successful reconciliation, application acceptance, and verified backup retention.
-- This permanently removes the private source staging copy from the target database.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM migration_source.import_batches
        WHERE batch_name = 'supabase-phase-5-v1'
    ) THEN
        RAISE EXCEPTION 'Expected completed Phase 5 import marker not found; refusing cleanup.';
    END IF;
END $$;

DROP SCHEMA migration_source CASCADE;
