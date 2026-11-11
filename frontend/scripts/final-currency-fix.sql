-- Final Currency Column Fix Script
-- This script adds currency columns to all necessary tables and fixes the schema

BEGIN;

-- Add currency column to loans table if it doesn't exist
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'loans' AND column_name = 'currency' AND table_schema = 'public'
    ) THEN
        ALTER TABLE public.loans ADD COLUMN currency TEXT DEFAULT 'INR' CHECK (currency IN ('INR', 'USD'));
        RAISE NOTICE 'Added currency column to loans table';
    ELSE
        RAISE NOTICE 'Currency column already exists in loans table';
    END IF;
END $$;

-- Add currency column to loan_payments table if it doesn't exist
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'loan_payments' AND column_name = 'currency' AND table_schema = 'public'
    ) THEN
        ALTER TABLE public.loan_payments ADD COLUMN currency TEXT DEFAULT 'INR' CHECK (currency IN ('INR', 'USD'));
        RAISE NOTICE 'Added currency column to loan_payments table';
    ELSE
        RAISE NOTICE 'Currency column already exists in loan_payments table';
    END IF;
END $$;

-- Ensure income table has currency column (should be USD)
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'income' AND column_name = 'currency' AND table_schema = 'public'
    ) THEN
        ALTER TABLE public.income ADD COLUMN currency TEXT DEFAULT 'USD' CHECK (currency = 'USD');
        RAISE NOTICE 'Added currency column to income table';
    ELSE
        RAISE NOTICE 'Currency column already exists in income table';
    END IF;
END $$;

-- Ensure international_transfers has proper currency columns
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'international_transfers' AND column_name = 'sent_currency' AND table_schema = 'public'
    ) THEN
        ALTER TABLE public.international_transfers ADD COLUMN sent_currency TEXT DEFAULT 'USD' CHECK (sent_currency IN ('INR', 'USD', 'EUR', 'GBP', 'CAD', 'AUD'));
        RAISE NOTICE 'Added sent_currency column to international_transfers table';
    END IF;
    
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'international_transfers' AND column_name = 'received_currency' AND table_schema = 'public'
    ) THEN
        ALTER TABLE public.international_transfers ADD COLUMN received_currency TEXT DEFAULT 'INR' CHECK (received_currency IN ('INR', 'USD', 'EUR', 'GBP', 'CAD', 'AUD'));
        RAISE NOTICE 'Added received_currency column to international_transfers table';
    END IF;
END $$;

-- Update existing records to have proper currency values
UPDATE public.loans SET currency = 'INR' WHERE currency IS NULL;
UPDATE public.loan_payments SET currency = 'INR' WHERE currency IS NULL;
UPDATE public.income SET currency = 'USD' WHERE currency IS NULL;
UPDATE public.international_transfers SET sent_currency = 'USD' WHERE sent_currency IS NULL;
UPDATE public.international_transfers SET received_currency = 'INR' WHERE received_currency IS NULL;

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_loans_currency ON public.loans(currency);
CREATE INDEX IF NOT EXISTS idx_loan_payments_currency ON public.loan_payments(currency);
CREATE INDEX IF NOT EXISTS idx_income_currency ON public.income(currency);

-- Verify the changes
SELECT 'loans' as table_name, 
       EXISTS(SELECT 1 FROM information_schema.columns WHERE table_name = 'loans' AND column_name = 'currency') as has_currency_column
UNION ALL
SELECT 'loan_payments' as table_name,
       EXISTS(SELECT 1 FROM information_schema.columns WHERE table_name = 'loan_payments' AND column_name = 'currency') as has_currency_column
UNION ALL
SELECT 'income' as table_name,
       EXISTS(SELECT 1 FROM information_schema.columns WHERE table_name = 'income' AND column_name = 'currency') as has_currency_column
UNION ALL
SELECT 'international_transfers' as table_name,
       EXISTS(SELECT 1 FROM information_schema.columns WHERE table_name = 'international_transfers' AND column_name = 'sent_currency') as has_currency_column;

COMMIT;

-- Final success message
SELECT 'SUCCESS: All currency columns have been added and configured!' as status;
