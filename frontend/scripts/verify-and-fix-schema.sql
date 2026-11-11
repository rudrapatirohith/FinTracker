-- Complete schema verification and fix script
-- This script will check and fix all missing columns and constraints

-- 1. Fix loans table
DO $$ 
BEGIN
    -- Add currency column if missing
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'loans' AND column_name = 'currency' AND table_schema = 'public'
    ) THEN
        ALTER TABLE public.loans ADD COLUMN currency TEXT DEFAULT 'INR' CHECK (currency IN ('INR', 'USD'));
        UPDATE public.loans SET currency = 'INR' WHERE currency IS NULL;
        RAISE NOTICE 'Added currency column to loans table';
    END IF;
END $$;

-- 2. Fix loan_payments table
DO $$ 
BEGIN
    -- Add currency column if missing
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'loan_payments' AND column_name = 'currency' AND table_schema = 'public'
    ) THEN
        ALTER TABLE public.loan_payments ADD COLUMN currency TEXT DEFAULT 'INR' CHECK (currency IN ('INR', 'USD'));
        UPDATE public.loan_payments SET currency = 'INR' WHERE currency IS NULL;
        RAISE NOTICE 'Added currency column to loan_payments table';
    END IF;
END $$;

-- 3. Ensure income table has proper currency constraint (USD only)
DO $$ 
BEGIN
    -- Check if currency column exists and has proper constraint
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'income' AND column_name = 'currency' AND table_schema = 'public'
    ) THEN
        -- Update all income records to USD
        UPDATE public.income SET currency = 'USD' WHERE currency IS NULL OR currency != 'USD';
        RAISE NOTICE 'Updated income currency to USD';
    END IF;
END $$;

-- 4. Verify international_transfers table structure
DO $$ 
BEGIN
    -- Ensure proper currency columns exist
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'international_transfers' AND column_name = 'currency_sent' AND table_schema = 'public'
    ) THEN
        ALTER TABLE public.international_transfers ADD COLUMN currency_sent TEXT DEFAULT 'USD';
        RAISE NOTICE 'Added currency_sent column to international_transfers table';
    END IF;
    
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'international_transfers' AND column_name = 'currency_received' AND table_schema = 'public'
    ) THEN
        ALTER TABLE public.international_transfers ADD COLUMN currency_received TEXT DEFAULT 'INR';
        RAISE NOTICE 'Added currency_received column to international_transfers table';
    END IF;
END $$;

-- 5. Create indexes if they don't exist
CREATE INDEX IF NOT EXISTS idx_loans_currency ON public.loans(currency);
CREATE INDEX IF NOT EXISTS idx_loan_payments_currency ON public.loan_payments(currency);
CREATE INDEX IF NOT EXISTS idx_income_currency ON public.income(currency);

-- 6. Verify all tables have proper RLS policies
DO $$
BEGIN
    -- Check if RLS is enabled on all tables
    IF NOT EXISTS (
        SELECT 1 FROM pg_class c
        JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE c.relname = 'loans' AND n.nspname = 'public' AND c.relrowsecurity = true
    ) THEN
        ALTER TABLE public.loans ENABLE ROW LEVEL SECURITY;
        RAISE NOTICE 'Enabled RLS on loans table';
    END IF;
    
    IF NOT EXISTS (
        SELECT 1 FROM pg_class c
        JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE c.relname = 'loan_payments' AND n.nspname = 'public' AND c.relrowsecurity = true
    ) THEN
        ALTER TABLE public.loan_payments ENABLE ROW LEVEL SECURITY;
        RAISE NOTICE 'Enabled RLS on loan_payments table';
    END IF;
END $$;

-- 7. Show final table structures
SELECT 'loans' as table_name, column_name, data_type, column_default, is_nullable
FROM information_schema.columns 
WHERE table_name = 'loans' AND table_schema = 'public'
UNION ALL
SELECT 'loan_payments' as table_name, column_name, data_type, column_default, is_nullable
FROM information_schema.columns 
WHERE table_name = 'loan_payments' AND table_schema = 'public'
UNION ALL
SELECT 'income' as table_name, column_name, data_type, column_default, is_nullable
FROM information_schema.columns 
WHERE table_name = 'income' AND table_schema = 'public'
ORDER BY table_name, column_name;

-- 8. Show RLS status
SELECT 
    schemaname,
    tablename,
    rowsecurity as rls_enabled,
    hasrls as has_rls_policies
FROM pg_tables 
WHERE schemaname = 'public' 
AND tablename IN ('loans', 'loan_payments', 'income', 'international_transfers', 'profiles')
ORDER BY tablename;
